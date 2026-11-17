<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import ReportButton from "@/components/ReportButton.vue";
import ReportPanel from "./ReportPanel.vue";
import { api, ApiError } from "@/shared/api";
import { CONFIG_NEUTRE } from "@/shared/constants";
import { useFlux } from "@/shared/sse";
import type { Commande, ConfigBouton, OrigineAffichage, VarianteCourante } from "@/shared/types";
import { attendre, dansUnJeu, ecouter, envoyer, etatDuJeu, objet } from "./bridge";

/*
 * Overlay permanent, chargé par le jeu dans une iframe transparente plein écran :
 *   /overlay/?session=42&user=Storm_X9
 * - reçoit variantes et commandes de l'API (SSE /api/sessions/{id}/flux) ;
 * - confirme chaque variante affichée (affichage), relaie apparitions de bugs et interactions ;
 * - envoie les signalements reliés à l'affichage et à l'apparition qu'ils couvrent.
 */

const params = new URLSearchParams(location.search);
const session = Number(params.get("session"));
const pseudo = (params.get("user") ?? "").trim();
const configure = Number.isInteger(session) && session > 0 && pseudo.length > 0;

/* ── Variante courante ── */
const config = ref<ConfigBouton>(CONFIG_NEUTRE);
const varianteId = ref<number | null>(null);
const affichageId = ref<number | null>(null);

/* ── Apparitions de bugs pas encore signalées (badge « compteur » et lien des signalements) ── */
interface Apparition { id: number | null; titre: string; signalee: boolean }
const apparitions = ref<Apparition[]>([]);
const nonSignalees = computed(() => apparitions.value.filter((a) => !a.signalee).length);

const panneau = ref(false);
const dock = ref<HTMLElement | null>(null);
const erreurApi = ref("");

let fileAffichage = Promise.resolve();

/** Applique une variante puis confirme à l'API qu'elle est réellement affichée. */
function appliquer(v: VarianteCourante) {
  config.value = v.config;
  varianteId.value = v.variante_id;
  fileAffichage = fileAffichage.then(() => confirmer(v.variante_id, v.config, v.origine));
}

async function confirmer(id: number | null, c: ConfigBouton, origine: OrigineAffichage) {
  await nextTick();
  await new Promise(requestAnimationFrame);          // rendu effectif à l'écran
  try {
    affichageId.value = (await api.confirmerAffichage(session, { variante_id: id, config: c, origine })).id;
    erreurApi.value = "";
  } catch (e) {
    erreurApi.value = e instanceof Error ? e.message : String(e);
  }
}

/* ── Flux de l'API ── */
const flux = configure
  ? useFlux(`/api/sessions/${session}/flux`, { variante: appliquer, commande: executerCommande })
  : null;

async function executerCommande(c: Commande) {
  if (c.type !== "DECLENCHER_BUG" || c.statut !== "ENVOYEE") return;
  const titre = c.parametres.titre;
  let succes = false;
  let message: string | null = null;
  let payload: Record<string, unknown> = {};
  if (!dansUnJeu) {
    message = "Overlay ouvert hors d'un jeu.";
  } else {
    envoyer("trigger-bug", { commande_id: c.id, titre });
    try {
      const rep = await attendre((m) => m.type === "trigger-result" && m.commande_id === c.id, 5000);
      succes = rep.succes === true;
      message = succes ? null : String(rep.message ?? "Refusé par le jeu.");
      payload = objet(rep.payload);
    } catch (e) {
      message = (e as Error).message;
    }
  }
  try {
    await api.resultatCommande(c.id, succes, message);
  } catch { /* la commande restera ENVOYEE : visible dans la console */ }
  if (succes) await enregistrerApparition(titre, "COMMANDE", c.id, payload);
}

async function enregistrerApparition(titre: string, origine: "JEU" | "COMMANDE", commandeId: number | null, payload: Record<string, unknown>) {
  const app: Apparition = { id: null, titre, signalee: false };
  apparitions.value.push(app);
  try {
    app.id = (await api.signalerApparition(session, { titre, origine, commande_id: commandeId, affichage_id: affichageId.value, payload })).id;
  } catch (e) {
    erreurApi.value = e instanceof Error ? e.message : String(e);
  }
}

/* ── Messages spontanés du jeu ── */
const stopEcoute = ecouter((m) => {
  if (m.type === "bug" && typeof m.titre === "string" && configure) {
    enregistrerApparition(m.titre, "JEU", null, objet(m.payload));
  }
});

/* ── Interactions ── */
let dernierSurvol = 0;
function survol() {
  const t = Date.now();
  if (!configure || t - dernierSurvol < 4000) return;          // un survol toutes les 4 s au plus
  dernierSurvol = t;
  api.interaction(session, { type: "SURVOL", affichage_id: affichageId.value }).catch(() => {});
}

function ouvrir() {
  if (panneau.value) return;
  panneau.value = true;
  envoyer("report-opened");
  if (configure) {
    api.interaction(session, { type: "OUVERTURE", affichage_id: affichageId.value, details: { apparitions_non_signalees: nonSignalees.value } })
      .catch(() => {});
  }
}

function fermer(envoye: boolean) {
  panneau.value = false;
  envoyer("report-closed");
  if (!envoye && configure) api.interaction(session, { type: "ANNULATION", affichage_id: affichageId.value }).catch(() => {});
}

async function envoi(f: { titre: string; type_bug: string; description: string; comportement: Record<string, unknown> }) {
  // L'apparition couverte : la plus récente non signalée du même titre.
  const app = [...apparitions.value].reverse().find((a) => !a.signalee && a.titre === f.titre) ?? null;
  const etat = await etatDuJeu();
  try {
    const { id } = await api.creerBug(session, {
      titre: f.titre,
      type_bug: f.type_bug,
      description: f.description,
      id_utilisateur: pseudo,
      timestamp: new Date().toISOString(),
      payload: { ...etat, comportement: f.comportement },
      affichage_id: affichageId.value,
      apparition_id: app?.id ?? null,
    });
    if (app) app.signalee = true;
    envoyer("report-submitted", { id });
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) throw new Error("Session inconnue.");
    if (e instanceof ApiError && e.status === 400) throw new Error(`Signalement refusé : ${e.message}`);
    throw new Error("L'envoi a échoué. Réessaie dans un instant.");
  }
}

/* ── Zones cliquables transmises au jeu (les clics ailleurs traversent l'overlay) ── */
const MARGE = 14;
let zones: { x: number; y: number; w: number; h: number }[] = [];
function publierZones() {
  zones = [];
  if (dock.value && config.value.visible) {
    const r = dock.value.getBoundingClientRect();
    zones.push({ x: r.x - MARGE, y: r.y - MARGE, w: r.width + 2 * MARGE, h: r.height + 2 * MARGE });
  }
  envoyer("zones", { zones, capture: panneau.value });
}

/**
 * L'iframe ne reçoit des mouvements que lorsqu'elle capte la souris (dans une zone, ou panneau ouvert).
 * Dès que le pointeur sort des zones, on rend la main au jeu. Pas de mémoire d'état ici : un état
 * « déjà sorti » ferait rater la sortie quand le pointeur traverse la marge sans toucher le bouton.
 */
let observateur: ResizeObserver | null = null;
function pointeur(e: PointerEvent) {
  if (panneau.value) return;
  const dedans = zones.some((z) => e.clientX >= z.x && e.clientX <= z.x + z.w && e.clientY >= z.y && e.clientY <= z.y + z.h);
  if (!dedans) envoyer("passthrough");
}
function sortie() { if (!panneau.value) envoyer("passthrough"); }

onMounted(() => {
  envoyer("ready", { version: "2.0.0" });
  observateur = new ResizeObserver(publierZones);
  if (dock.value) observateur.observe(dock.value);
  window.addEventListener("resize", publierZones);
  document.addEventListener("pointermove", pointeur);
  document.documentElement.addEventListener("pointerleave", sortie);
  publierZones();
});
onBeforeUnmount(() => {
  stopEcoute();
  observateur?.disconnect();
  window.removeEventListener("resize", publierZones);
  document.removeEventListener("pointermove", pointeur);
  document.documentElement.removeEventListener("pointerleave", sortie);
});
watch([config, panneau], () => nextTick(publierZones), { deep: true });
// La taille animée (transition) change la zone : on republie à la fin.
function finTransition() { publierZones(); }

/* ── Mode développement (overlay ouvert sans ?session=&user=) ── */
const devSession = ref("42");
const devPseudo = ref("Storm_X9");
function lancerDev() {
  location.search = new URLSearchParams({ session: devSession.value, user: devPseudo.value.trim() }).toString();
}
</script>

<template>
  <div class="overlay" :class="[`pos-${config.position}`, { capture: panneau }]">
    <Transition name="voile">
      <div v-if="panneau" class="voile" @click="fermer(false)" />
    </Transition>

    <div ref="dock" class="dock" @transitionend="finTransition">
      <Transition name="bouton" mode="out-in" @after-enter="publierZones" @after-leave="publierZones">
        <ReportButton
          v-if="config.visible && !panneau"
          :key="config.position"
          :config="config"
          :compteur="nonSignalees"
          @click="ouvrir"
          @survol="survol"
        />
      </Transition>
    </div>

    <Transition name="panneau">
      <ReportPanel v-if="panneau" class="panneau" :envoi="envoi" @fermer="fermer" />
    </Transition>

    <div v-if="configure && (!flux?.connecte.value || erreurApi)" class="etat" role="status">
      <span class="pt" />
      {{ erreurApi || "Connexion à Kapture…" }}
    </div>

    <form v-if="!configure" class="dev" @submit.prevent="lancerDev">
      <h1>Overlay Kapture — mode développement</h1>
      <p>Sans <code>?session=</code> et <code>?user=</code>, l'overlay ne peut pas se connecter.</p>
      <label>Session <input v-model="devSession" type="number" min="1"></label>
      <label>Pseudonyme <input v-model="devPseudo" maxlength="30"></label>
      <button type="submit">Ouvrir l'overlay</button>
    </form>
  </div>
</template>

<style>
/* Fond strictement transparent : l'iframe laisse voir le jeu. Pas de color-scheme (sinon fond opaque). */
html, body { margin: 0; height: 100%; background: transparent; overflow: hidden; }
</style>

<style scoped>
.overlay { position: fixed; inset: 0; pointer-events: none; font-family: "Inter", system-ui, -apple-system, "Segoe UI", sans-serif; }
.overlay > * { pointer-events: auto; }
.overlay > .etat { pointer-events: none; }

.dock { position: absolute; display: flex; }
.pos-bas-droite .dock { right: 24px; bottom: 24px; }
.pos-bas-gauche .dock { left: 24px; bottom: 24px; }
.pos-haut-droite .dock { right: 24px; top: 24px; }
.pos-haut-gauche .dock { left: 24px; top: 24px; }

.panneau { position: absolute; }
.pos-bas-droite .panneau { right: 16px; bottom: 16px; transform-origin: bottom right; }
.pos-bas-gauche .panneau { left: 16px; bottom: 16px; transform-origin: bottom left; }
.pos-haut-droite .panneau { right: 16px; top: 16px; transform-origin: top right; }
.pos-haut-gauche .panneau { left: 16px; top: 16px; transform-origin: top left; }

.voile { position: absolute; inset: 0; background: rgb(26 26 24 / 0.4); }

.etat {
  position: absolute; left: 50%; top: 12px; translate: -50% 0;
  display: flex; align-items: center; gap: 8px;
  padding: 5px 10px; border-radius: 6px;
  background: #ffffff; color: #4a4945; font-size: 12px; border: 1px solid #e3e2dd;
}
.pt { width: 7px; height: 7px; border-radius: 50%; background: #9a5a06; }

/* Transitions */
.voile-enter-active, .voile-leave-active { transition: opacity 0.25s; }
.voile-enter-from, .voile-leave-to { opacity: 0; }

.bouton-enter-active { transition: opacity 0.35s, scale 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
.bouton-leave-active { transition: opacity 0.18s, scale 0.18s; }
.bouton-enter-from, .bouton-leave-to { opacity: 0; scale: 0.4; }

.panneau-enter-active { transition: opacity 0.25s, transform 0.4s cubic-bezier(0.22, 1, 0.36, 1); }
.panneau-leave-active { transition: opacity 0.18s, transform 0.2s ease-in; }
.panneau-enter-from, .panneau-leave-to { opacity: 0; transform: scale(0.97) translateY(6px); }
@media (prefers-reduced-motion: reduce) {
  .bouton-enter-from, .bouton-leave-to, .panneau-enter-from, .panneau-leave-to { scale: 1; transform: none; }
}

.dev {
  position: absolute; left: 50%; top: 50%; translate: -50% -50%;
  width: min(380px, calc(100vw - 32px));
  display: grid; gap: 10px; padding: 20px; border-radius: 8px;
  background: #ffffff; color: #1a1a18; border: 1px solid #e3e2dd;
}
.dev h1 { margin: 0; font-size: 16px; font-weight: 600; }
.dev p { margin: 0; color: #4a4945; font-size: 13px; }
.dev label { display: grid; gap: 4px; font-size: 12px; font-weight: 500; color: #4a4945; }
.dev input { font: inherit; padding: 7px 10px; border-radius: 6px; border: 1px solid #d6d5cf; background: #fff; color: inherit; }
.dev button { font: inherit; font-weight: 500; padding: 8px; border-radius: 6px; border: 0; background: #1a1a18; color: #fff; cursor: pointer; }
</style>
