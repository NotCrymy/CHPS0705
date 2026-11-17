<script setup lang="ts">
import { computed, ref, watch } from "vue";
import ReportButton from "@/components/ReportButton.vue";
import { ANIMATIONS, CONFIG_NEUTRE, INTENTIONS, POSITIONS, TAILLES } from "@/shared/constants";
import type { ConfigBouton, Variante } from "@/shared/types";

/**
 * Choix d'une variante du catalogue ou réglage d'une configuration personnalisée,
 * avec aperçu en direct sur une scène de jeu factice.
 */
const props = defineProps<{
  variantes: Variante[]; courante: ConfigBouton | null; varianteCouranteId: number | null; occupe: boolean;
  /** Masque la scène d'aperçu (quand le vrai jeu est affiché à côté). */
  sansApercu?: boolean;
}>();
const emit = defineEmits<{ (e: "appliquer", choix: { variante_id: number } | { config: ConfigBouton }): void }>();

/** Copie profonde : structuredClone échoue sur les proxys réactifs de Vue. */
const copie = (c: ConfigBouton): ConfigBouton => JSON.parse(JSON.stringify(c));

const brouillon = ref<ConfigBouton>(copie(props.courante ?? CONFIG_NEUTRE));
const choisie = ref<number | null>(props.varianteCouranteId);

// Nouvelle session sélectionnée ou variante changée ailleurs : on se recale.
watch(() => [props.courante, props.varianteCouranteId] as const, ([c, id]) => {
  brouillon.value = copie(c ?? CONFIG_NEUTRE);
  choisie.value = id;
});

function choisir(v: Variante) {
  choisie.value = v.id;
  brouillon.value = copie(v.config);
}

/** Le brouillon correspond-il exactement à une variante du catalogue ? */
const correspondance = computed(() => {
  const v = props.variantes.find((x) => x.id === choisie.value);
  return v && JSON.stringify(v.config) === JSON.stringify(brouillon.value) ? v : null;
});

const modifie = computed(() =>
  JSON.stringify(brouillon.value) !== JSON.stringify(props.courante) || (correspondance.value?.id ?? null) !== props.varianteCouranteId);

function appliquer() {
  emit("appliquer", correspondance.value ? { variante_id: correspondance.value.id } : { config: copie(brouillon.value) });
}

const demoCompteur = ref(3);
</script>

<template>
  <div class="studio">
    <div class="catalogue" role="radiogroup" aria-label="Variantes du catalogue">
      <button
        v-for="(v, i) in variantes"
        :key="v.id"
        type="button"
        role="radio"
        class="preset"
        :class="{ actif: correspondance?.id === v.id, courante: varianteCouranteId === v.id }"
        :aria-checked="correspondance?.id === v.id"
        :style="{ '--i': i }"
        @click="choisir(v)"
      >
        <span class="mini">
          <ReportButton v-if="v.config.visible" :config="{ ...v.config, taille: 'S', libelle: '', position: 'bas-droite' }" :compteur="2" apercu />
          <span v-else class="masque" aria-hidden="true">⌀</span>
        </span>
        <span class="nom">{{ v.nom }}</span>
        <span class="chip" :class="INTENTIONS[v.intention].ton">{{ INTENTIONS[v.intention].label }}</span>
        <span v-if="varianteCouranteId === v.id" class="en-cours">en cours</span>
      </button>
    </div>

    <div class="atelier" :class="{ seul: sansApercu }">
      <div v-if="!sansApercu" class="scene" :class="`pos-${brouillon.position}`" aria-label="Aperçu en jeu">
        <div class="decor" aria-hidden="true"><i /><i /><i /><span class="perso" /></div>
        <Transition name="apercu" mode="out-in">
          <div v-if="brouillon.visible" :key="brouillon.position" class="ancre">
            <ReportButton :config="brouillon" :compteur="demoCompteur" apercu />
          </div>
          <p v-else class="cache">Bouton masqué : le joueur ne peut pas signaler.</p>
        </Transition>
        <span class="etiquette">Aperçu</span>
      </div>

      <div class="reglages">
        <label class="switch">
          <input v-model="brouillon.visible" type="checkbox">
          <span class="piste" aria-hidden="true" />
          Bouton visible
        </label>

        <div class="duo">
          <label class="field"><span>Couleur</span>
            <input v-model="brouillon.couleur" type="color">
          </label>
          <label class="field"><span>Opacité · {{ Math.round(brouillon.opacite * 100) }} %</span>
            <input v-model.number="brouillon.opacite" type="range" min="0.1" max="1" step="0.05">
          </label>
        </div>

        <div class="field">
          <span>Taille</span>
          <div class="segments">
            <button v-for="t in TAILLES" :key="t.value" type="button" :class="{ on: brouillon.taille === t.value }" @click="brouillon.taille = t.value">{{ t.value }}</button>
          </div>
        </div>

        <label class="field"><span>Libellé ({{ brouillon.libelle.length }}/40, vide = icône seule)</span>
          <input v-model="brouillon.libelle" maxlength="40" placeholder="Signaler un bug">
        </label>

        <div class="duo">
          <label class="field"><span>Animation</span>
            <select v-model="brouillon.animation">
              <option v-for="a in ANIMATIONS" :key="a.value" :value="a.value">{{ a.label }}</option>
            </select>
          </label>
          <label class="field"><span>Position</span>
            <select v-model="brouillon.position">
              <option v-for="p in POSITIONS" :key="p.value" :value="p.value">{{ p.label }}</option>
            </select>
          </label>
        </div>

        <div class="duo">
          <label class="field"><span>Badge</span>
            <select v-model="brouillon.badge.mode">
              <option value="aucun">Aucun</option>
              <option value="texte">Texte</option>
              <option value="compteur">Compteur de bugs non signalés</option>
            </select>
          </label>
          <label v-if="brouillon.badge.mode === 'texte'" class="field"><span>Texte du badge</span>
            <input v-model="brouillon.badge.texte" maxlength="3" placeholder="!">
          </label>
          <label v-else-if="brouillon.badge.mode === 'compteur'" class="field"><span>Aperçu : {{ demoCompteur }} bug(s)</span>
            <input v-model.number="demoCompteur" type="range" min="0" max="12">
          </label>
        </div>

        <div class="action">
          <span class="muted">{{ correspondance ? `Variante « ${correspondance.nom} »` : "Configuration personnalisée" }}</span>
          <button type="button" class="btn btn-primary" :disabled="!modifie || occupe" @click="appliquer">
            {{ occupe ? "Envoi…" : modifie ? "Appliquer en jeu" : "Déjà appliquée" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.catalogue { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 0.6rem; margin-bottom: 1rem; }
.preset {
  position: relative; display: grid; justify-items: start; gap: 0.35rem;
  padding: 0.75rem; border-radius: var(--radius-sm); cursor: pointer; text-align: left;
  font: inherit; color: var(--ink);
  background: var(--surface); border: 1px solid var(--line);
  transition: border-color 0.15s, background-color 0.15s;
}
.preset:hover { border-color: var(--line-strong); background: var(--bg-2); }
.preset.actif { border-color: var(--accent); box-shadow: inset 0 0 0 1px var(--accent); }
.mini { height: 46px; display: flex; align-items: center; padding-left: 4px; }
.masque { font-size: 1.4rem; color: var(--muted); }
.nom { font-weight: 600; }
.en-cours { position: absolute; top: 0.6rem; right: 0.65rem; font-size: 0.75rem; font-weight: 500; color: var(--accent); }
@media (prefers-reduced-motion: no-preference) {
  .preset { animation: apparait 0.4s var(--ease-out) both; animation-delay: calc(var(--i) * 40ms); }
}
@keyframes apparait { from { opacity: 0; } }

.studio { container-type: inline-size; }
.atelier { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr); gap: 1rem; }
.atelier.seul { grid-template-columns: 1fr; }
@container (max-width: 640px) { .atelier { grid-template-columns: 1fr; } }

.scene {
  position: relative; min-height: 300px; border-radius: var(--radius-sm); overflow: hidden;
  background: #2b2d31;
}
.decor i { position: absolute; background: #44474e; }
.decor i:nth-child(1) { left: 18%; top: 15%; width: 10px; height: 45%; }
.decor i:nth-child(2) { left: 40%; top: 62%; width: 35%; height: 10px; }
.decor i:nth-child(3) { left: 72%; top: 18%; width: 10px; height: 30%; }
.decor { position: absolute; inset: 0; background-image: linear-gradient(rgb(255 255 255 / 0.04) 1px, transparent 1px), linear-gradient(90deg, rgb(255 255 255 / 0.04) 1px, transparent 1px); background-size: 28px 28px; }
.perso { position: absolute; left: 30%; top: 45%; width: 18px; height: 18px; border-radius: 50%; background: #e5c07b; }
.ancre { position: absolute; }
.pos-bas-droite .ancre { right: 18px; bottom: 18px; }
.pos-bas-gauche .ancre { left: 18px; bottom: 18px; }
.pos-haut-droite .ancre { right: 18px; top: 18px; }
.pos-haut-gauche .ancre { left: 18px; top: 18px; }
.cache { position: absolute; inset: auto 0 1rem; margin: 0; text-align: center; font-size: 0.8125rem; color: #c9cbd0; }
.etiquette { position: absolute; left: 50%; top: 10px; translate: -50% 0; font-size: 0.6875rem; font-weight: 500; letter-spacing: 0.06em; text-transform: uppercase; color: #c9cbd0; }
.apercu-enter-active { transition: opacity 0.3s, scale 0.45s cubic-bezier(0.34, 1.56, 0.64, 1); }
.apercu-leave-active { transition: opacity 0.15s, scale 0.15s; }
.apercu-enter-from, .apercu-leave-to { opacity: 0; scale: 0.5; }

.reglages { display: grid; gap: 0.85rem; align-content: start; }
.duo { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.segments { display: flex; border-radius: var(--radius-sm); border: 1px solid var(--line-strong); overflow: hidden; }
.segments button {
  flex: 1; font: inherit; font-weight: 500; padding: 0.4rem; border: 0; cursor: pointer;
  background: var(--surface); color: var(--ink-2); transition: background-color 0.15s, color 0.15s;
}
.segments button + button { border-left: 1px solid var(--line); }
.segments button:hover:not(.on) { background: var(--surface-2); }
.segments button.on { background: var(--ink); color: #fff; }

.switch { display: flex; align-items: center; gap: 0.6rem; font-weight: 500; cursor: pointer; width: fit-content; }
.switch input { position: absolute; opacity: 0; }
.piste { width: 36px; height: 20px; border-radius: 999px; background: #c9c8c2; position: relative; transition: background-color 0.2s; }
.piste::after { content: ""; position: absolute; left: 3px; top: 3px; width: 14px; height: 14px; border-radius: 50%; background: #fff; transition: translate 0.25s var(--ease-out); }
.switch input:checked + .piste { background: var(--ink); }
.switch input:checked + .piste::after { translate: 16px 0; }
.switch input:focus-visible + .piste { outline: 2px solid var(--accent); outline-offset: 2px; }

.action { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding-top: 0.75rem; border-top: 1px solid var(--line); font-size: 0.8125rem; }
</style>
