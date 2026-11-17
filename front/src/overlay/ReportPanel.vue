<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { TITRES, TYPES_BUG } from "@/shared/constants";

/** Formulaire de signalement. Le parent fait l'envoi (il connaît session, variante, apparitions). */
const props = defineProps<{ envoi: (f: { titre: string; type_bug: string; description: string; comportement: Record<string, unknown> }) => Promise<void> }>();
const emit = defineEmits<{ (e: "fermer", envoye: boolean): void }>();

const titre = ref("");
const typeBug = ref("");
const description = ref("");
const erreur = ref("");
const envoiEnCours = ref(false);
const termine = ref(false);
const premier = ref<HTMLElement | null>(null);

/* Métriques de comportement, invisibles pour le joueur (comme en v1). */
const ouvertA = performance.now();
let premiereSaisieA: number | null = null;
let modifications = 0;
const ordreSaisie: string[] = [];
function saisie(champ: string) {
  modifications += 1;
  premiereSaisieA ??= performance.now();
  if (!ordreSaisie.includes(champ)) ordreSaisie.push(champ);
}

const valide = computed(() => titre.value !== "" && typeBug.value !== "" && description.value.length <= 500);

onMounted(() => nextTick(() => premier.value?.focus()));

async function envoyer() {
  erreur.value = "";
  if (!valide.value) {
    erreur.value = !titre.value ? "Choisis le bug rencontré." : !typeBug.value ? "Choisis une catégorie." : "Description trop longue.";
    return;
  }
  envoiEnCours.value = true;
  try {
    await props.envoi({
      titre: titre.value,
      type_bug: typeBug.value,
      description: description.value.trim(),
      comportement: {
        tempsAvantPremiereSaisieMs: premiereSaisieA === null ? null : Math.round(premiereSaisieA - ouvertA),
        tempsAvantEnvoiMs: Math.round(performance.now() - ouvertA),
        nombreModifications: modifications,
        ordreSaisie,
      },
    });
    termine.value = true;
    setTimeout(() => emit("fermer", true), 1700);
  } catch (e) {
    erreur.value = e instanceof Error ? e.message : "L'envoi a échoué. Réessaie dans un instant.";
  } finally {
    envoiEnCours.value = false;
  }
}
</script>

<template>
  <div class="panel" role="dialog" aria-modal="true" aria-labelledby="kp-titre" @keydown.esc="emit('fermer', termine)">
    <Transition name="swap" mode="out-in">
      <form v-if="!termine" key="form" novalidate @submit.prevent="envoyer">
        <header>
          <div>
            <h1 id="kp-titre">Un problème dans le jeu&nbsp;?</h1>
            <p>Dis-nous ce que tu viens de voir.</p>
          </div>
          <button type="button" class="x" aria-label="Fermer" @click="emit('fermer', false)">×</button>
        </header>

        <fieldset>
          <legend>Bug rencontré</legend>
          <div class="choix grille">
            <label v-for="(t, i) in TITRES" :key="t.value" class="opt" :style="{ '--i': i }">
              <input
                :ref="i === 0 ? (el) => (premier = el as HTMLElement) : undefined"
                v-model="titre" type="radio" name="titre" :value="t.value" @change="saisie('titre')"
              >
              <span><b>{{ t.value }}</b>{{ t.label }}</span>
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Catégorie</legend>
          <div class="choix puces">
            <label v-for="(t, i) in TYPES_BUG" :key="t.value" class="opt puce" :style="{ '--i': i + 5 }">
              <input v-model="typeBug" type="radio" name="type_bug" :value="t.value" @change="saisie('type_bug')">
              <span :title="t.label">{{ t.court }}</span>
            </label>
          </div>
        </fieldset>

        <label class="desc">
          <span>Description <i>(facultatif)</i></span>
          <textarea v-model="description" rows="2" maxlength="500" @input="saisie('description')" />
          <small>{{ description.length }} / 500</small>
        </label>

        <Transition name="err">
          <p v-if="erreur" class="erreur" role="alert">{{ erreur }}</p>
        </Transition>

        <footer>
          <button type="button" class="b" @click="emit('fermer', false)">Annuler</button>
          <button type="submit" class="b b1" :disabled="envoiEnCours">
            <span v-if="envoiEnCours" class="spin" aria-hidden="true" />
            {{ envoiEnCours ? "Envoi…" : "Envoyer" }}
          </button>
        </footer>
      </form>

      <div v-else key="ok" class="merci" role="status">
        <svg viewBox="0 0 52 52" aria-hidden="true">
          <circle cx="26" cy="26" r="24" />
          <path d="M15 27l7 7 15-16" />
        </svg>
        <h1>Merci&nbsp;!</h1>
        <p>Ton signalement a bien été envoyé.</p>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* Panneau de signalement, style Atelier : carte blanche, encre sombre, un seul accent. */
.panel {
  width: min(420px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  overflow: auto;
  padding: 20px;
  border-radius: 8px;
  color: #1a1a18;
  background: #ffffff;
  border: 1px solid #e3e2dd;
  box-shadow: 0 2px 4px rgb(0 0 0 / 0.12), 0 16px 40px -12px rgb(0 0 0 / 0.4);
  font: 14px/1.45 "Inter", system-ui, -apple-system, "Segoe UI", sans-serif;
}
header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 16px; }
h1 { margin: 0; font-size: 17px; font-weight: 600; letter-spacing: -0.01em; }
header p, .merci p { margin: 2px 0 0; color: #5d5c57; font-size: 13px; }
.x {
  flex: none; width: 30px; height: 30px; border-radius: 6px; border: 1px solid transparent; cursor: pointer;
  background: transparent; color: #4a4945; font-size: 20px; line-height: 1;
  transition: background-color 0.15s;
}
.x:hover { background: #f4f4f1; }

fieldset { border: 0; padding: 0; margin: 0 0 14px; }
legend, .desc > span { font-size: 12px; font-weight: 500; color: #4a4945; margin-bottom: 6px; padding: 0; }
.desc i { font-style: normal; color: #6e6d68; }

.choix { display: grid; gap: 6px; }
.grille { grid-template-columns: 1fr 1fr; }
.puces { display: flex; flex-wrap: wrap; }
.opt { position: relative; cursor: pointer; }
.opt input { position: absolute; opacity: 0; inset: 0; margin: 0; cursor: pointer; }
.opt span {
  display: flex; flex-direction: column; gap: 1px;
  height: 100%;
  padding: 8px 10px;
  border-radius: 6px;
  background: #ffffff;
  border: 1px solid #d6d5cf;
  color: #5d5c57;
  font-size: 12.5px;
  transition: background-color 0.15s, border-color 0.15s, color 0.15s;
}
.opt b { color: #1a1a18; font-size: 13px; font-weight: 600; }
.puce span { flex-direction: row; padding: 5px 10px; }
.opt:hover span { background: #fafaf8; border-color: #bdbcb5; }
.opt input:checked + span {
  background: #eef1fc;
  border-color: #2c4bd6;
  box-shadow: inset 0 0 0 1px #2c4bd6;
  color: #1e36a3;
}
.opt input:focus-visible + span { outline: 2px solid #2c4bd6; outline-offset: 2px; }

.desc { display: flex; flex-direction: column; margin-bottom: 6px; }
textarea {
  font: inherit; color: inherit; resize: vertical; min-height: 56px;
  padding: 8px 10px; border-radius: 6px;
  background: #ffffff;
  border: 1px solid #d6d5cf;
  transition: border-color 0.15s, box-shadow 0.15s;
}
textarea:focus { outline: none; border-color: #2c4bd6; box-shadow: 0 0 0 3px #eef1fc; }
.desc small { align-self: flex-end; color: #6e6d68; font-size: 11px; margin-top: 3px; }

.erreur { margin: 6px 0 0; padding: 8px 11px; border-radius: 6px; background: #fcebe9; border: 1px solid #f1c4bf; color: #b42318; }

footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; padding-top: 14px; border-top: 1px solid #e3e2dd; }
.b {
  font: inherit; font-weight: 500; cursor: pointer;
  display: inline-flex; align-items: center; gap: 8px;
  min-height: 36px; padding: 0 14px; border-radius: 6px;
  border: 1px solid #d6d5cf; background: #ffffff; color: #1a1a18;
  transition: background-color 0.15s, border-color 0.15s;
}
.b:hover:not(:disabled) { background: #f4f4f1; }
.b:disabled { opacity: 0.6; cursor: wait; }
.b1 { background: #1a1a18; border-color: #1a1a18; color: #fff; }
.b1:hover:not(:disabled) { background: #33332f; }
.b:focus-visible, .x:focus-visible { outline: 2px solid #2c4bd6; outline-offset: 2px; }

.spin { width: 14px; height: 14px; border-radius: 50%; border: 2px solid rgb(255 255 255 / 0.35); border-top-color: #fff; }

.merci { text-align: center; padding: 24px 8px 16px; }
.merci svg { width: 56px; height: 56px; margin-bottom: 10px; }
.merci circle { fill: none; stroke: #1f7a4d; stroke-width: 3; }
.merci path { fill: none; stroke: #1f7a4d; stroke-width: 4; stroke-linecap: round; stroke-linejoin: round; }

/* Transitions */
.swap-enter-active, .swap-leave-active { transition: opacity 0.22s, scale 0.22s cubic-bezier(0.22, 1, 0.36, 1); }
.swap-enter-from, .swap-leave-to { opacity: 0; scale: 0.99; }
.err-enter-active, .err-leave-active { transition: opacity 0.2s, translate 0.2s; }
.err-enter-from, .err-leave-to { opacity: 0; translate: 0 -4px; }

@media (prefers-reduced-motion: no-preference) {
  .opt { animation: entree 0.4s cubic-bezier(0.22, 1, 0.36, 1) both; animation-delay: calc(var(--i) * 25ms + 80ms); }
  .spin { animation: tourne 0.7s linear infinite; }
  .merci circle { stroke-dasharray: 151; stroke-dashoffset: 151; animation: trace 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards; }
  .merci path { stroke-dasharray: 36; stroke-dashoffset: 36; animation: trace 0.35s 0.5s cubic-bezier(0.65, 0, 0.45, 1) forwards; }
  .merci h1, .merci p { animation: entree 0.45s 0.55s cubic-bezier(0.22, 1, 0.36, 1) both; }
}
@keyframes entree { from { opacity: 0; } }
@keyframes tourne { to { rotate: 360deg; } }
@keyframes trace { to { stroke-dashoffset: 0; } }
</style>
