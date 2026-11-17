<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "@/shared/api";
import { INTENTIONS, TITRES } from "@/shared/constants";
import type { ConfigBouton } from "@/shared/types";
import StudioVariante from "../components/StudioVariante.vue";
import FluxDirect from "../components/FluxDirect.vue";
import { chargerSessions, entrees, erreurs, sessions, variantes } from "../live";

const route = useRoute();
const router = useRouter();

const triees = computed(() => [...sessions.value].sort((a, b) => Number(b.connecte) - Number(a.connecte) || a.id - b.id));
const selection = computed(() => {
  const id = Number(route.query.session);
  return sessions.value.find((s) => s.id === id) ?? triees.value[0] ?? null;
});
const choisir = (id: number) => router.replace({ query: { session: String(id) } });

const fluxSession = computed(() => entrees.value.filter((e) => e.session_id === selection.value?.id).slice(0, 80));
const intentionCourante = computed(() => {
  const v = variantes.value.find((x) => x.id === selection.value?.variante?.variante_id);
  return v ? INTENTIONS[v.intention] : null;
});

/* ── Notifications ── */
const toasts = ref<{ id: number; ton: string; texte: string }[]>([]);
let idToast = 0;
function toast(ton: string, texte: string) {
  const id = ++idToast;
  toasts.value.push({ id, ton, texte });
  setTimeout(() => (toasts.value = toasts.value.filter((t) => t.id !== id)), 4000);
}

/* ── Variante ── */
const envoiVariante = ref(false);
async function appliquer(choix: { variante_id: number } | { config: ConfigBouton }) {
  const s = selection.value;
  if (!s) return;
  envoiVariante.value = true;
  try {
    const etat = await api.assignerVariante(s.id, choix);
    const i = sessions.value.findIndex((x) => x.id === s.id);
    if (i >= 0) sessions.value[i] = etat;
    toast("ok", etat.connecte ? "Variante envoyée à l'overlay." : "Variante enregistrée : elle s'appliquera à la connexion de l'overlay.");
  } catch (e) {
    toast("err", (e as Error).message);
  } finally {
    envoiVariante.value = false;
  }
}

/* ── Commandes ── */
const enCours = ref<string | null>(null);
async function declencher(titre: string) {
  const s = selection.value;
  if (!s) return;
  enCours.value = titre;
  try {
    await api.declencherBug(s.id, titre);
    toast("info", `${titre} demandé au jeu…`);
  } catch (e) {
    toast("err", (e as Error).message);
    chargerSessions();
  } finally {
    enCours.value = null;
  }
}

/** Dernier statut connu de commande par titre (le flux renvoie la commande à chaque changement). */
const statuts = computed(() => {
  const m = new Map<string, string>();
  for (const e of fluxSession.value) {
    if (e.type !== "COMMANDE") continue;
    const t = e.details?.parametres?.titre;
    if (t && !m.has(t)) m.set(t, e.details.statut);
  }
  return m;
});
</script>

<template>
  <div class="pilotage">
    <section class="col sessions card" aria-labelledby="t-sessions">
      <div class="card-head"><h2 id="t-sessions">Sessions</h2><span class="muted">{{ sessions.length }}</span></div>
      <div class="card-body">
        <p v-if="erreurs.sessions" class="alert err">{{ erreurs.sessions }}</p>
        <div v-else-if="!sessions.length" class="squelettes">
          <div v-for="i in 5" :key="i" class="skeleton" style="height: 58px" />
        </div>
        <TransitionGroup v-else tag="ul" name="list" class="liste">
          <li v-for="s in triees" :key="s.id">
            <button type="button" class="session" :class="{ actif: selection?.id === s.id }" @click="choisir(s.id)">
              <span class="dot" :class="{ on: s.connecte }" :title="s.connecte ? 'Overlay connecté' : 'Hors ligne'" />
              <span class="id">S{{ s.id }}</span>
              <span class="qui"><b>{{ s.id_utilisateur }}</b><small>{{ s.application }}</small></span>
              <span class="var mono">{{ s.variante?.code ?? (s.variante ? "PERSO" : "—") }}</span>
            </button>
          </li>
        </TransitionGroup>
      </div>
    </section>

    <div v-if="selection" class="col centre">
      <header class="entete">
        <div>
          <h1>Session {{ selection.id }} <span class="muted">· {{ selection.id_utilisateur }}</span></h1>
          <p class="muted">
            {{ selection.application }} ·
            <span :class="selection.connecte ? 'ok-txt' : ''">{{ selection.connecte ? `overlay connecté (${selection.overlays})` : "aucun overlay connecté" }}</span>
            <template v-if="selection.variante"> · variante <b class="mono">{{ selection.variante.code ?? "personnalisée" }}</b></template>
            <span v-if="intentionCourante" class="chip" :class="intentionCourante.ton">{{ intentionCourante.label }}</span>
          </p>
        </div>
        <a class="btn" :href="`/demo/?session=${selection.id}&user=${encodeURIComponent(selection.id_utilisateur)}`" target="_blank" rel="noopener">
          Ouvrir le jeu de démo ↗
        </a>
      </header>

      <section class="card" aria-labelledby="t-bouton">
        <div class="card-head">
          <h2 id="t-bouton">Bouton de signalement</h2>
          <span class="muted">Inciter ou dissuader le joueur de signaler</span>
        </div>
        <div class="card-body">
          <p v-if="erreurs.variantes" class="alert err">{{ erreurs.variantes }}</p>
          <StudioVariante
            :variantes="variantes"
            :courante="selection.variante?.config ?? null"
            :variante-courante-id="selection.variante?.variante_id ?? null"
            :occupe="envoiVariante"
            @appliquer="appliquer"
          />
        </div>
      </section>

      <section class="card" aria-labelledby="t-bugs">
        <div class="card-head">
          <h2 id="t-bugs">Déclencher un bug dans le jeu</h2>
          <span v-if="!selection.connecte" class="chip warn">Overlay hors ligne</span>
        </div>
        <div class="card-body declencheurs">
          <button
            v-for="t in TITRES" :key="t.value" type="button" class="declencheur" :class="`n-${t.nature.replace('é', 'e')}`"
            :disabled="!selection.connecte || enCours !== null" @click="declencher(t.value)"
          >
            <b>{{ t.value }}</b>
            <span>{{ t.label }}</span>
            <small class="nature">{{ t.nature }}</small>
            <Transition name="fade" mode="out-in">
              <small v-if="statuts.get(t.value)" :key="statuts.get(t.value)" class="statut" :class="statuts.get(t.value)">
                {{ { ENVOYEE: "envoyée…", EXECUTEE: "exécutée ✓", ECHEC: "échec ✕" }[statuts.get(t.value)!] }}
              </small>
            </Transition>
          </button>
        </div>
      </section>
    </div>
    <div v-else class="col centre"><p class="alert info">Aucune session. Lance l'API et la base (voir README).</p></div>

    <section class="col droite card" aria-labelledby="t-flux">
      <div class="card-head"><h2 id="t-flux">En direct</h2><span v-if="selection" class="muted">S{{ selection.id }}</span></div>
      <div class="card-body"><FluxDirect :entrees="fluxSession" /></div>
    </section>

    <div class="toasts" aria-live="polite">
      <TransitionGroup name="toast">
        <div v-for="t in toasts" :key="t.id" class="alert" :class="t.ton">{{ t.texte }}</div>
      </TransitionGroup>
    </div>
  </div>
</template>

<style scoped>
.pilotage { display: grid; grid-template-columns: 270px minmax(0, 1fr) 320px; gap: 1.25rem; align-items: start; }
@media (max-width: 1200px) { .pilotage { grid-template-columns: 240px minmax(0, 1fr); } .droite { grid-column: 1 / -1; } }
@media (max-width: 760px) { .pilotage { grid-template-columns: 1fr; } }
.col { min-width: 0; }
.sessions, .droite { position: sticky; top: 76px; max-height: calc(100vh - 100px); overflow: auto; }
@media (max-width: 1200px) { .droite { position: static; max-height: none; } }
@media (max-width: 760px) { .sessions { position: static; max-height: none; } }
.centre { display: grid; gap: 1.25rem; }

.sessions .card-body { padding: 0.35rem; }
.liste { list-style: none; margin: 0; padding: 0; display: grid; gap: 2px; position: relative; }
.liste li { width: 100%; }
.squelettes { display: grid; gap: 0.4rem; }
.session {
  width: 100%; display: grid; grid-template-columns: auto auto 1fr auto; align-items: center; gap: 0.6rem;
  padding: 0.55rem 0.6rem; border-radius: var(--radius-sm); cursor: pointer; text-align: left;
  font: inherit; color: var(--ink); background: transparent; border: 0;
  transition: background-color 0.15s;
}
.session:hover { background: var(--surface-2); }
.session.actif { background: var(--accent-soft); }
.session.actif .var { color: var(--accent); }
.id { font-weight: 600; color: var(--ink-2); font-variant-numeric: tabular-nums; }
.qui { display: grid; min-width: 0; }
.qui b { font-weight: 600; overflow: hidden; text-overflow: ellipsis; }
.qui small { color: var(--muted); font-size: 0.75rem; }
.var { font-size: 0.68rem; color: var(--ink-2); max-width: 90px; overflow: hidden; text-overflow: ellipsis; }

.entete { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; }
.entete p { margin: 0.25rem 0 0; display: flex; gap: 0.4rem; align-items: center; flex-wrap: wrap; }
.ok-txt { color: var(--ok); }

.declencheurs { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 0.6rem; }
.declencheur {
  position: relative; display: grid; gap: 0.15rem; justify-items: start; text-align: left;
  padding: 0.75rem; border-radius: var(--radius-sm); cursor: pointer; font: inherit; color: var(--ink);
  background: var(--surface); border: 1px solid var(--line-strong);
  transition: border-color 0.15s, background-color 0.15s;
}
.declencheur:hover:not(:disabled) { background: var(--surface-2); border-color: #bdbcb5; }
.declencheur:active:not(:disabled) { background: var(--surface-3); }
.declencheur:disabled { opacity: 0.5; cursor: not-allowed; }
.declencheur b { font-family: var(--mono); font-weight: 500; }
.declencheur span { font-size: 0.8125rem; color: var(--ink-2); }
.nature { font-size: 0.75rem; color: var(--muted); }
.n-avantageux .nature { color: var(--ok); }
.n-desavantageux .nature { color: var(--err); }
.statut { position: absolute; top: 0.7rem; right: 0.7rem; font-size: 0.75rem; font-weight: 600; }
.statut.ENVOYEE { color: var(--warn); }
.statut.EXECUTEE { color: var(--ok); }
.statut.ECHEC { color: var(--err); }

.toasts { position: fixed; right: 1.25rem; bottom: 1.25rem; display: grid; gap: 0.5rem; z-index: 20; width: min(360px, calc(100vw - 2.5rem)); }
.toasts .alert { box-shadow: var(--shadow); }
.toast-enter-active { transition: opacity 0.3s, translate 0.4s var(--ease-out); }
.toast-leave-active { transition: opacity 0.25s, translate 0.25s; }
.toast-enter-from { opacity: 0; translate: 0 6px; }
.toast-leave-to { opacity: 0; }
</style>
