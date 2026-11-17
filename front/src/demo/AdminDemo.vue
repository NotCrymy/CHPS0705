<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";
import { api } from "@/shared/api";
import { INTENTIONS, TITRES } from "@/shared/constants";
import { useFlux } from "@/shared/sse";
import type { ConfigBouton, EntreeChronologie, SessionEtat, Variante } from "@/shared/types";
import StudioVariante from "@/console/components/StudioVariante.vue";
import FluxDirect from "@/console/components/FluxDirect.vue";

/*
 * Panneau opérateur affiché à côté du jeu de démo : c'est la console Kapture réduite à une session.
 * Tout passe par l'API, exactement comme depuis la console : variante → SSE → overlay,
 * bug → commande → overlay → jeu.
 */
const props = defineProps<{ session: number }>();

const etat = ref<SessionEtat | null>(null);
const variantes = ref<Variante[]>([]);
const entrees = ref<EntreeChronologie[]>([]);
const erreur = ref("");
const message = ref<{ ton: string; texte: string } | null>(null);
const envoi = ref(false);
const enCours = ref<string | null>(null);

async function chargerEtat() {
  try {
    const toutes = await api.sessions();
    etat.value = toutes.find((s) => s.id === props.session) ?? null;
    erreur.value = etat.value ? "" : `Session ${props.session} inconnue de l'API.`;
  } catch (e) {
    erreur.value = (e as Error).message;
  }
}
api.variantes().then((v) => (variantes.value = v)).catch((e) => (erreur.value = (e as Error).message));
chargerEtat();

// Flux de la console, filtré sur la session du jeu.
let rechargement = 0;
const { connecte: enDirect } = useFlux("/api/flux", {
  chronologie: (e: EntreeChronologie) => {
    if (e.session_id !== props.session) return;
    entrees.value = [e, ...entrees.value].slice(0, 60);
    if (e.type === "AFFICHAGE" || e.type === "CONNEXION") {
      clearTimeout(rechargement);
      rechargement = window.setTimeout(chargerEtat, 150);
    }
  },
});
onBeforeUnmount(() => clearTimeout(rechargement));

let minuteurMessage = 0;
function annoncer(ton: string, texte: string) {
  message.value = { ton, texte };
  clearTimeout(minuteurMessage);
  minuteurMessage = window.setTimeout(() => (message.value = null), 3500);
}

async function appliquer(choix: { variante_id: number } | { config: ConfigBouton }) {
  envoi.value = true;
  try {
    etat.value = await api.assignerVariante(props.session, choix);
    annoncer("ok", "Variante envoyée au jeu.");
  } catch (e) {
    annoncer("err", (e as Error).message);
  } finally {
    envoi.value = false;
  }
}

async function declencher(titre: string) {
  enCours.value = titre;
  try {
    await api.declencherBug(props.session, titre);
  } catch (e) {
    annoncer("err", (e as Error).message);
    chargerEtat();
  } finally {
    enCours.value = null;
  }
}

/** Dernier statut connu de commande par bug (le flux renvoie la commande à chaque changement). */
const statuts = computed(() => {
  const m = new Map<string, string>();
  for (const e of entrees.value) {
    if (e.type !== "COMMANDE") continue;
    const t = e.details?.parametres?.titre;
    if (t && !m.has(t)) m.set(t, e.details.statut);
  }
  return m;
});
const LIBELLES_STATUT: Record<string, string> = { ENVOYEE: "envoyée…", EXECUTEE: "exécutée ✓", ECHEC: "échec ✕" };

const intention = computed(() => {
  const v = variantes.value.find((x) => x.id === etat.value?.variante?.variante_id);
  return v ? INTENTIONS[v.intention] : null;
});
</script>

<template>
  <div class="admin">
    <header class="tete">
      <div class="titre">
        <span class="logo" aria-hidden="true">K</span>
        <div>
          <h1>Panneau opérateur</h1>
          <p class="muted">Session {{ session }}<template v-if="etat"> · {{ etat.id_utilisateur }}</template></p>
        </div>
      </div>
      <span class="etat" :title="enDirect ? 'Flux en direct actif' : 'Flux en direct coupé'">
        <span class="dot" :class="{ on: etat?.connecte }" />
        {{ etat?.connecte ? "overlay connecté" : "overlay hors ligne" }}
      </span>
    </header>

    <p v-if="erreur" class="alert err">{{ erreur }}</p>

    <section class="bloc" aria-labelledby="a-variante">
      <div class="bloc-tete">
        <h2 id="a-variante">Bouton de signalement</h2>
        <span v-if="etat?.variante" class="courante">
          <b class="mono">{{ etat.variante.code ?? "PERSONNALISÉE" }}</b>
          <span v-if="intention" class="chip" :class="intention.ton">{{ intention.label }}</span>
        </span>
      </div>
      <StudioVariante
        :variantes="variantes"
        :courante="etat?.variante?.config ?? null"
        :variante-courante-id="etat?.variante?.variante_id ?? null"
        :occupe="envoi"
        sans-apercu
        @appliquer="appliquer"
      />
    </section>

    <section class="bloc" aria-labelledby="a-bugs">
      <div class="bloc-tete">
        <h2 id="a-bugs">Déclencher un bug</h2>
        <span class="muted petit">via l'API → overlay → jeu</span>
      </div>
      <div class="declencheurs">
        <button
          v-for="t in TITRES" :key="t.value" type="button" class="declencheur" :class="`n-${t.nature.replace('é', 'e')}`"
          :disabled="!etat?.connecte || enCours !== null" :title="t.label" @click="declencher(t.value)"
        >
          <b>{{ t.value }}</b>
          <span>{{ t.label }}</span>
          <Transition name="fade" mode="out-in">
            <small v-if="statuts.get(t.value)" :key="statuts.get(t.value)" class="statut" :class="statuts.get(t.value)">
              {{ LIBELLES_STATUT[statuts.get(t.value)!] }}
            </small>
          </Transition>
        </button>
      </div>
    </section>

    <section class="bloc" aria-labelledby="a-flux">
      <div class="bloc-tete"><h2 id="a-flux">En direct</h2></div>
      <FluxDirect :entrees="entrees" vide="Les variantes affichées, bugs et signalements de cette session apparaîtront ici." />
    </section>

    <Transition name="rise">
      <div v-if="message" class="alert message" :class="message.ton" role="status">{{ message.texte }}</div>
    </Transition>
  </div>
</template>

<style scoped>
.admin { display: grid; gap: 0.75rem; padding: 1rem 1rem 1.5rem; align-content: start; }
.tete { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
.titre { display: flex; gap: 0.6rem; align-items: center; }
.logo {
  width: 22px; height: 22px; border-radius: 5px; display: grid; place-items: center; font-size: 0.75rem; font-weight: 700; color: #fff;
  background: var(--ink);
}
h1 { margin: 0; font-size: 0.9375rem; letter-spacing: -0.01em; }
.tete p { margin: 0; font-size: 0.8125rem; }
.etat { display: inline-flex; align-items: center; gap: 0.45rem; font-size: 0.8125rem; color: var(--ink-2); }

.bloc { border-radius: var(--radius); background: var(--surface); border: 1px solid var(--line); }
.bloc > :not(.bloc-tete) { margin: 0.9rem; }
.bloc-tete { display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; padding: 0.75rem 0.9rem; border-bottom: 1px solid var(--line); flex-wrap: wrap; }
h2 { margin: 0; font-size: 0.875rem; font-weight: 600; }
.courante { display: inline-flex; gap: 0.4rem; align-items: center; font-size: 0.8125rem; }
.petit { font-size: 0.8125rem; }

.declencheurs { display: grid; grid-template-columns: repeat(auto-fill, minmax(118px, 1fr)); gap: 0.5rem; }
.declencheur {
  position: relative; display: grid; gap: 0.1rem; justify-items: start; text-align: left;
  padding: 0.6rem 0.7rem; border-radius: var(--radius-sm); cursor: pointer; font: inherit; color: var(--ink);
  background: var(--surface); border: 1px solid var(--line-strong);
  transition: background-color 0.15s, border-color 0.15s;
}
.declencheur b { font-family: var(--mono); font-weight: 500; }
.declencheur.n-avantageux b { color: var(--ok); }
.declencheur.n-desavantageux b { color: var(--err); }
.declencheur:hover:not(:disabled) { background: var(--surface-2); border-color: #bdbcb5; }
.declencheur:disabled { opacity: 0.5; cursor: not-allowed; }
.declencheur span { font-size: 0.8125rem; color: var(--ink-2); }
.statut { position: absolute; top: 0.6rem; right: 0.6rem; font-size: 0.75rem; font-weight: 600; }
.statut.ENVOYEE { color: var(--warn); }
.statut.EXECUTEE { color: var(--ok); }
.statut.ECHEC { color: var(--err); }

.message { position: sticky; bottom: 0.75rem; box-shadow: var(--shadow); }
</style>
