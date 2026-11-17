<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "@/shared/api";
import { ENTREES, INTENTIONS } from "@/shared/constants";
import { duree, heure, pourcent } from "@/shared/format";
import type { EntreeChronologie, StatVariante } from "@/shared/types";
import { sessions, surEntree } from "../live";
import { resume } from "../resume";

const route = useRoute();
const router = useRouter();
const sessionId = computed<number | undefined>({
  get: () => (route.query.session ? Number(route.query.session) : undefined),
  set: (v) => router.replace({ query: v ? { session: String(v) } : {} }),
});

const stats = ref<StatVariante[] | null>(null);
const chrono = ref<EntreeChronologie[] | null>(null);
const erreur = ref("");

async function charger() {
  try {
    const [s, c] = await Promise.all([
      api.suiviVariantes(sessionId.value),
      sessionId.value ? api.chronologie(sessionId.value) : Promise.resolve(null),
    ]);
    stats.value = s;
    chrono.value = c;
    erreur.value = "";
  } catch (e) {
    erreur.value = (e as Error).message;
  }
}
watch(sessionId, () => { stats.value = null; chrono.value = null; charger(); }, { immediate: true });

// Mise à jour en direct (regroupée) quand quelque chose se passe.
let minuteur = 0;
const stop = surEntree((e) => {
  if (sessionId.value && e.session_id !== sessionId.value) return;
  clearTimeout(minuteur);
  minuteur = window.setTimeout(charger, 600);
});
onBeforeUnmount(() => { stop(); clearTimeout(minuteur); });

const totaux = computed(() => {
  const s = stats.value ?? [];
  const somme = (k: keyof StatVariante) => s.reduce((a, x) => a + (Number(x[k]) || 0), 0);
  const apparitions = somme("apparitions");
  const couvrants = somme("signalements_couvrant_apparition");
  return {
    affichages: somme("affichages"),
    apparitions,
    signalements: somme("signalements"),
    taux: apparitions ? couvrants / apparitions : null,
  };
});

const triees = computed(() => [...(stats.value ?? [])].sort((a, b) => (b.taux_signalement ?? -1) - (a.taux_signalement ?? -1)));

/** Chronologie découpée en tranches : une par variante affichée. */
const tranches = computed(() => {
  const res: { cle: string; entete: EntreeChronologie | null; items: EntreeChronologie[] }[] = [];
  for (const e of chrono.value ?? []) {
    if (e.type === "AFFICHAGE" || !res.length) res.push({ cle: `${e.type}-${e.id}`, entete: e.type === "AFFICHAGE" ? e : null, items: [] });
    if (e.type !== "AFFICHAGE") res[res.length - 1].items.push(e);
  }
  return res.reverse();
});
</script>

<template>
  <div class="suivi">
    <header class="entete">
      <div>
        <h1>Suivi des variantes</h1>
        <p class="muted">Chaque variante affichée est enregistrée : apparitions, interactions et signalements y sont rattachés.</p>
      </div>
      <label class="field filtre">
        <span>Session</span>
        <select v-model="sessionId">
          <option :value="undefined">Toutes les sessions</option>
          <option v-for="s in sessions" :key="s.id" :value="s.id">S{{ s.id }} · {{ s.id_utilisateur }} · {{ s.application }}</option>
        </select>
      </label>
    </header>

    <p v-if="erreur" class="alert err">{{ erreur }}</p>

    <div class="tuiles">
      <div v-for="(t, i) in [
        { l: 'Affichages', v: totaux.affichages, aide: '' },
        { l: 'Bugs apparus', v: totaux.apparitions, aide: '' },
        { l: 'Signalements', v: totaux.signalements, aide: '' },
        { l: 'Taux de signalement', v: pourcent(totaux.taux), aide: 'apparitions couvertes par un signalement' },
      ]" :key="t.l" class="card tuile" :style="{ '--i': i }">
        <small>{{ t.l }}</small>
        <Transition name="fade" mode="out-in"><b :key="String(t.v)">{{ stats ? t.v : "…" }}</b></Transition>
        <span v-if="t.aide" class="muted">{{ t.aide }}</span>
      </div>
    </div>

    <section class="card" aria-labelledby="t-var">
      <div class="card-head"><h2 id="t-var">Par variante</h2><span class="muted">trié par taux de signalement</span></div>
      <div class="card-body tableau">
        <div v-if="!stats" class="skeleton" style="height: 180px" />
        <p v-else-if="!stats.length" class="muted">Aucun affichage enregistré pour l'instant.</p>
        <table v-else>
          <thead>
            <tr>
              <th scope="col">Variante</th>
              <th scope="col" class="taux">Taux de signalement</th>
              <th scope="col" class="n">Affichages</th>
              <th scope="col" class="n">Durée</th>
              <th scope="col" class="n">Apparitions</th>
              <th scope="col" class="n">Signalements</th>
              <th scope="col" class="n">Survols</th>
              <th scope="col" class="n">Ouvertures</th>
              <th scope="col" class="n">Abandons</th>
            </tr>
          </thead>
          <TransitionGroup tag="tbody" name="list">
            <tr v-for="s in triees" :key="s.code + (s.variante_id ?? '')">
              <th scope="row">
                <span class="nom">{{ s.nom }}</span>
                <span v-if="s.intention" class="chip" :class="INTENTIONS[s.intention].ton">{{ INTENTIONS[s.intention].label }}</span>
                <span class="mono muted code">{{ s.code }}</span>
              </th>
              <td class="taux">
                <div class="barre" :title="`${s.signalements_couvrant_apparition} apparition(s) signalée(s) sur ${s.apparitions}`">
                  <i :style="{ scale: `${s.taux_signalement ?? 0} 1` }" />
                </div>
                <b>{{ pourcent(s.taux_signalement) }}</b>
              </td>
              <td class="n">{{ s.affichages }}</td>
              <td class="n">{{ duree(s.duree_ms) }}</td>
              <td class="n">{{ s.apparitions }}</td>
              <td class="n">{{ s.signalements }}<small v-if="s.signalements !== s.signalements_couvrant_apparition" class="muted"> ({{ s.signalements_couvrant_apparition }} liés)</small></td>
              <td class="n">{{ s.survols }}</td>
              <td class="n">{{ s.ouvertures }}</td>
              <td class="n">{{ s.annulations }}</td>
            </tr>
          </TransitionGroup>
        </table>
      </div>
    </section>

    <section class="card" aria-labelledby="t-chrono">
      <div class="card-head"><h2 id="t-chrono">Chronologie</h2><span class="muted">la plus récente en haut</span></div>
      <div class="card-body">
        <p v-if="!sessionId" class="muted">Choisis une session pour voir sa chronologie.</p>
        <div v-else-if="!chrono" class="skeleton" style="height: 160px" />
        <p v-else-if="!chrono.length" class="muted">Rien d'enregistré pour cette session.</p>
        <TransitionGroup v-else tag="ol" name="list" class="frise">
          <li v-for="t in tranches" :key="t.cle" class="tranche">
            <div v-if="t.entete" class="cause">
              <span class="pastille" aria-hidden="true">◐</span>
              <div>
                <b>{{ resume(t.entete).titre }}</b>
                <span class="muted"> · {{ resume(t.entete).detail }} · {{ heure(t.entete.timestamp) }}</span>
              </div>
            </div>
            <ol class="effets">
              <li v-for="e in [...t.items].reverse()" :key="`${e.type}-${e.id}-${e.timestamp}`" :class="`t-${resume(e).ton}`">
                <span class="ico" aria-hidden="true">{{ ENTREES[e.type].icone }}</span>
                <span><b>{{ resume(e).titre }}</b> <span class="muted">{{ resume(e).detail }}</span></span>
                <time class="mono muted">{{ heure(e.timestamp) }}</time>
              </li>
              <li v-if="!t.items.length" class="vide muted">Aucun événement pendant cette variante.</li>
            </ol>
          </li>
        </TransitionGroup>
      </div>
    </section>
  </div>
</template>

<style scoped>
.suivi { display: grid; gap: 1.25rem; }
.entete { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; }
.entete p { margin: 0.3rem 0 0; }
.filtre { min-width: 280px; }

.tuiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; }
.tuile { padding: 1rem 1.15rem; display: grid; gap: 0.15rem; }
.tuile small { font-size: 0.8125rem; font-weight: 500; color: var(--ink-2); }
.tuile b { font-size: 1.75rem; font-weight: 600; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; }
.tuile span { font-size: 0.78rem; }
@media (prefers-reduced-motion: no-preference) {
  .tuile { animation: monte 0.45s var(--ease-out) both; animation-delay: calc(var(--i) * 60ms); }
}
@keyframes monte { from { opacity: 0; } }

.tableau { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: 0.88rem; position: relative; }
th, td { padding: 0.65rem 0.6rem; text-align: left; border-bottom: 1px solid var(--line); white-space: nowrap; }
thead th { font-size: 0.8125rem; color: var(--ink-2); font-weight: 600; border-bottom-color: var(--line-strong); }
tbody th { font-weight: 500; }
.nom { font-weight: 600; margin-right: 0.4rem; }
.code { display: block; font-size: 0.7rem; margin-top: 0.15rem; }
.n { text-align: right; font-variant-numeric: tabular-nums; }
.taux { min-width: 210px; }
td.taux { display: flex; align-items: center; gap: 0.6rem; }
.barre { flex: 1; height: 6px; border-radius: 3px; background: var(--surface-3); overflow: hidden; }
.barre i { display: block; height: 100%; border-radius: 3px; background: var(--accent); transform-origin: left; transition: scale 0.8s var(--ease-out); }
td.taux b { min-width: 3.2rem; text-align: right; font-variant-numeric: tabular-nums; }

.frise { list-style: none; margin: 0; padding: 0; display: grid; gap: 1rem; position: relative; }
.tranche { position: relative; padding-left: 1rem; border-left: 2px solid var(--line-strong); width: 100%; }
.cause { display: flex; gap: 0.6rem; align-items: center; margin-bottom: 0.5rem; margin-left: -1.65rem; }
.pastille { width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center; background: var(--ink); color: #fff; font-size: 0.75rem; font-weight: 600; box-shadow: 0 0 0 4px var(--surface); }
.effets { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.3rem; }
.effets li { display: grid; grid-template-columns: 24px 1fr auto; gap: 0.5rem; align-items: center; padding: 0.35rem 0.55rem; border-radius: var(--radius-sm); background: var(--bg-2); border: 1px solid var(--line); }
.effets li.vide { display: block; background: none; }
.ico { width: 24px; height: 24px; border-radius: 4px; display: grid; place-items: center; background: var(--surface-3); font-size: 0.8rem; }
.t-ok .ico { background: var(--ok-soft); color: var(--ok); }
.t-warn .ico { background: var(--warn-soft); color: var(--warn); }
.t-err .ico { background: var(--err-soft); color: var(--err); }
.effets time { font-size: 0.75rem; }
</style>
