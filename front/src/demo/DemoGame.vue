<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, shallowRef } from "vue";
import { BUGS, DUREES, Jeu } from "./jeu";
import { TITRES } from "@/shared/constants";
import AdminDemo from "./AdminDemo.vue";

/*
 * Jeu de démonstration : il joue le rôle d'un vrai jeu qui intègre Kapture avec le SDK
 * (/sdk/kapture-overlay.js, chargé par une balise <script> comme le ferait n'importe quel jeu).
 */

interface SdkKapture { bugAppeared(titre: string, payload?: object): void; destroy(): void }
declare global {
  interface Window { KaptureOverlay: { mount(o: Record<string, unknown>): SdkKapture } }
}

const params = new URLSearchParams(location.search);
const session = ref(params.get("session") ?? "42");
const pseudo = ref(params.get("user") ?? "Storm_X9");

const canvas = ref<HTMLCanvasElement | null>(null);
const zoneJeu = ref<HTMLElement | null>(null);
const panneau = ref(true);
const jeu = shallowRef<Jeu | null>(null);
const hud = reactive({ score: 0, vies: 3, niveau: 1, effets: [] as { titre: string; restant: number; total: number }[] });
const coulisses = ref(false);
const spontanes = ref(true);
const journal = ref<{ id: number; heure: string; sens: string; texte: string }[]>([]);
let sdk: SdkKapture | null = null;
let idJournal = 0;
let minuteur = 0;

function rafraichir() {
  const j = jeu.value;
  if (!j) return;
  hud.score = j.score;
  hud.vies = j.vies;
  hud.niveau = j.niveau;
  hud.effets = [...j.effets.keys()].map((titre) => ({ titre, restant: j.restant(titre), total: DUREES[titre] }));
}

function tracer(sens: string, msg: Record<string, unknown>) {
  const { source: _source, ...reste } = msg;
  journal.value.unshift({ id: ++idJournal, heure: new Date().toLocaleTimeString("fr-FR"), sens, texte: JSON.stringify(reste) });
  if (journal.value.length > 60) journal.value.pop();
}

onMounted(() => {
  const j = new Jeu(canvas.value!, {
    change: rafraichir,
    bugSpontane: (titre) => sdk?.bugAppeared(titre, j.etat()),
  });
  jeu.value = j;
  sdk = window.KaptureOverlay.mount({
    session: session.value,
    user: pseudo.value,
    baseUrl: location.origin,
    container: zoneJeu.value,   // l'overlay couvre la zone de jeu, pas le panneau opérateur
    getState: () => j.etat(),
    onTriggerBug: (titre: string) => j.declencher(titre),
    onReportOpened: () => (j.pause = true),
    onReportClosed: () => (j.pause = false),
    onMessage: tracer,
  });
  minuteur = window.setInterval(rafraichir, 200);
  canvas.value!.focus();
});

onBeforeUnmount(() => {
  clearInterval(minuteur);
  sdk?.destroy();
  jeu.value?.detruire();
});

function basculerSpontanes() {
  if (jeu.value) jeu.value.spontanes = spontanes.value;
}

function faireApparaitre(titre: string) {
  const j = jeu.value!;
  j.declencher(titre);
  sdk?.bugAppeared(titre, j.etat());
}

function changerSession() {
  location.search = new URLSearchParams({ session: session.value, user: pseudo.value.trim() }).toString();
}

const nomBug = (t: string) => TITRES.find((x) => x.value === t)?.label ?? t;
</script>

<template>
  <div class="page" :class="{ replie: !panneau }">
  <main ref="zoneJeu" class="scene">
    <canvas ref="canvas" tabindex="0" aria-label="Jeu de démonstration : déplace-toi avec les flèches ou ZQSD" />

    <div class="hud">
      <div class="stat"><small>Score</small><b>{{ hud.score }}</b></div>
      <div class="stat"><small>Niveau</small><b>{{ hud.niveau }}</b></div>
      <div class="stat vies" :aria-label="`${hud.vies} vies`">
        <small>Vies</small>
        <b><TransitionGroup name="coeur"><span v-for="i in hud.vies" :key="i">♥</span></TransitionGroup></b>
      </div>
      <TransitionGroup name="effet" tag="div" class="effets">
        <div v-for="e in hud.effets" :key="e.titre" class="effet">
          <span>{{ e.titre }} · {{ nomBug(e.titre) }}</span>
          <i :style="{ scale: `${e.restant / e.total} 1` }" />
        </div>
      </TransitionGroup>
    </div>

    <button type="button" class="ouvrir" :aria-expanded="coulisses" @click="coulisses = !coulisses">
      {{ coulisses ? "Fermer les coulisses" : "Coulisses" }}
    </button>

    <Transition name="tiroir">
      <aside v-if="coulisses" class="tiroir">
        <h2>Coulisses du jeu de démo</h2>
        <p class="aide">Ce panneau simule ce que fait un vrai jeu. Déplacement : flèches / ZQSD. Le bouton Kapture est dessiné par l'overlay, par-dessus.</p>

        <form class="ligne" @submit.prevent="changerSession">
          <label>Session <input v-model="session" type="number" min="1"></label>
          <label>Pseudonyme <input v-model="pseudo" maxlength="30"></label>
          <button type="submit">Relancer</button>
        </form>

        <label class="bascule">
          <input v-model="spontanes" type="checkbox" @change="basculerSpontanes">
          Bugs spontanés aléatoires (toutes les 18–40 s)
        </label>

        <h3>Faire apparaître un bug maintenant</h3>
        <div class="bugs">
          <button v-for="b in BUGS" :key="b" type="button" :title="nomBug(b)" @click="faireApparaitre(b)">{{ b }}</button>
        </div>

        <h3>Échanges avec l'overlay</h3>
        <TransitionGroup tag="ol" name="log" class="log">
          <li v-for="l in journal" :key="l.id">
            <time>{{ l.heure }}</time> <em :class="l.sens.startsWith('←') ? 'in' : 'out'">{{ l.sens }}</em>
            <code>{{ l.texte }}</code>
          </li>
        </TransitionGroup>
      </aside>
    </Transition>

    <button type="button" class="replier" :aria-expanded="panneau" aria-controls="panneau-operateur" @click="panneau = !panneau">
      {{ panneau ? "Masquer le panneau →" : "← Panneau opérateur" }}
    </button>
  </main>

  <aside v-show="panneau" id="panneau-operateur" class="operateur" aria-label="Panneau opérateur Kapture">
    <AdminDemo :session="Number(session)" />
  </aside>
  </div>
</template>

<style>
html, body { margin: 0; height: 100%; background: #1f2023; overflow: hidden; }
</style>

<style scoped>
.page { position: fixed; inset: 0; display: grid; grid-template-columns: minmax(0, 1fr) clamp(360px, 32vw, 460px); }
.page.replie { grid-template-columns: minmax(0, 1fr); }
@media (max-width: 860px) {
  .page { grid-template-columns: 1fr; grid-template-rows: 56dvh minmax(0, 1fr); }
  .page.replie { grid-template-rows: 1fr; }
}
/* Zone de jeu : conteneur de l'iframe Kapture (isolée pour que son z-index reste dans la zone). */
.scene { position: relative; min-width: 0; min-height: 0; isolation: isolate; overflow: hidden; color: #e8e8e5; }
.operateur { overflow: auto; overscroll-behavior: contain; background: var(--bg); border-left: 1px solid var(--line); }
@media (max-width: 860px) { .operateur { border-left: 0; border-top: 1px solid var(--line); } }

/* Contrôles posés sur le jeu : sombres et mats, comme le jeu. */
.replier, .ouvrir {
  position: absolute; z-index: 2;
  font: inherit; font-size: 0.8125rem; font-weight: 500; cursor: pointer;
  padding: 0.4rem 0.75rem; border-radius: var(--radius-sm);
  background: #1c1d21; color: #e8e8e5; border: 1px solid #3a3c42;
  transition: background-color 0.15s, border-color 0.15s;
}
.replier:hover, .ouvrir:hover { background: #26272c; border-color: #55585f; }
.replier { left: 50%; bottom: 14px; translate: -50% 0; }
.ouvrir { left: 16px; top: 16px; }
canvas { width: 100%; height: 100%; display: block; outline: none; }

.hud { position: absolute; left: 50%; top: 14px; translate: -50% 0; display: flex; gap: 8px; align-items: flex-start; pointer-events: none; }
@media (max-width: 600px) { .hud { top: 60px; } }
.stat {
  min-width: 72px; padding: 5px 12px; border-radius: var(--radius-sm);
  background: rgb(28 29 33 / 0.9); border: 1px solid #3a3c42;
  display: grid; text-align: center;
}
.stat small { font-size: 0.6875rem; color: #a3a5ac; }
.stat b { font-family: var(--mono); font-size: 1.125rem; font-weight: 500; }
.vies b { color: #e7a59b; display: flex; justify-content: center; gap: 2px; font-family: var(--sans); }
.effets { display: grid; gap: 6px; }
.effet {
  position: relative; overflow: hidden; padding: 5px 12px; border-radius: var(--radius-sm); font-size: 0.75rem; font-weight: 500;
  background: rgb(28 29 33 / 0.9); border: 1px solid #3a3c42;
}
.effet i { position: absolute; left: 0; bottom: 0; height: 2px; width: 100%; background: #e5c07b; transform-origin: left; transition: scale 0.2s linear; }

/* Coulisses : panneau clair Atelier, comme la console. */
.tiroir {
  position: absolute; left: 16px; top: 60px; bottom: 16px; width: min(380px, calc(100vw - 32px));
  overflow: auto; padding: 1rem; border-radius: var(--radius);
  background: var(--surface); color: var(--ink); border: 1px solid var(--line); box-shadow: var(--shadow);
}
h2 { margin: 0 0 4px; font-size: 0.9375rem; }
h3 { margin: 1rem 0 0.5rem; font-size: 0.8125rem; font-weight: 600; color: var(--ink); }
.aide { margin: 0 0 0.75rem; color: var(--ink-2); font-size: 0.8125rem; }
.ligne { display: grid; grid-template-columns: 1fr 1.4fr auto; gap: 8px; align-items: end; }
label { display: grid; gap: 3px; font-size: 0.75rem; font-weight: 500; color: var(--ink-2); }
input:not([type="checkbox"]) { min-width: 0; padding: 0.35rem 0.55rem; }
input[type="checkbox"] { width: auto; accent-color: var(--ink); }
button { font: inherit; cursor: pointer; }
.ligne button, .bugs button {
  padding: 0.35rem 0.7rem; border-radius: var(--radius-sm); border: 1px solid var(--line-strong);
  background: var(--surface); color: var(--ink); font-weight: 500;
  transition: background-color 0.15s;
}
.bugs button { font-family: var(--mono); }
.ligne button:hover, .bugs button:hover { background: var(--surface-2); }
.bugs button:active { background: var(--surface-3); }
.bascule { display: flex; gap: 8px; align-items: center; margin-top: 0.75rem; font-size: 0.8125rem; color: var(--ink); }
.bugs { display: flex; gap: 6px; flex-wrap: wrap; }
.log { list-style: none; margin: 0; padding: 0; display: grid; font-size: 0.75rem; }
.log li { padding: 0.35rem 0; border-bottom: 1px solid var(--line); word-break: break-all; }
.log time { font-family: var(--mono); color: var(--muted); }
.log em { font-style: normal; font-weight: 600; }
.log em.in { color: var(--accent); }
.log em.out { color: var(--ink); }
.log code { color: var(--ink-2); }

.tiroir-enter-active, .tiroir-leave-active { transition: opacity 0.25s, translate 0.3s cubic-bezier(0.22, 1, 0.36, 1); }
.tiroir-enter-from, .tiroir-leave-to { opacity: 0; translate: -8px 0; }
.effet-enter-active, .effet-leave-active { transition: opacity 0.3s, translate 0.3s; }
.effet-enter-from, .effet-leave-to { opacity: 0; translate: 0 -6px; }
.coeur-leave-active { transition: opacity 0.4s, scale 0.4s; }
.coeur-leave-to { opacity: 0; scale: 2; }
.coeur-enter-active { transition: scale 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.coeur-enter-from { scale: 0; }
.log-enter-active { transition: opacity 0.3s, translate 0.3s; }
.log-enter-from { opacity: 0; translate: 0 -4px; }
</style>
