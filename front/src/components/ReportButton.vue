<script setup lang="ts">
import { computed } from "vue";
import type { ConfigBouton } from "@/shared/types";

/**
 * Bouton de signalement rendu selon une variante (ConfigBouton).
 * Partagé par l'overlay (vrai bouton) et la console (aperçu en direct).
 */
const props = withDefaults(defineProps<{ config: ConfigBouton; compteur?: number; apercu?: boolean }>(), {
  compteur: 0,
  apercu: false,
});
defineEmits<{ (e: "click"): void; (e: "survol"): void }>();

const DIMENSIONS = { S: 36, M: 48, L: 60, XL: 76 } as const;

/** Texte noir ou blanc selon la luminance de la couleur. */
function encre(hex: string) {
  const n = Number.parseInt(hex.slice(1), 16);
  const [r, g, b] = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((c) => {
    const s = c / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b > 0.45 ? "#0b0d12" : "#ffffff";
}

const style = computed(() => ({
  "--c": props.config.couleur,
  "--ink": encre(props.config.couleur),
  "--h": `${DIMENSIONS[props.config.taille]}px`,
  "--o": String(props.config.opacite),
}));

const badge = computed(() => {
  const b = props.config.badge;
  if (b.mode === "texte") return b.texte || null;
  if (b.mode === "compteur") return props.compteur > 0 ? (props.compteur > 9 ? "9+" : String(props.compteur)) : null;
  return null;
});

const etiquette = computed(() => props.config.libelle || "Signaler un bug");
</script>

<template>
  <button
    type="button"
    class="kb"
    :class="[`anim-${config.animation}`, { 'icone-seule': !config.libelle, apercu }]"
    :style="style"
    :aria-label="config.libelle ? undefined : etiquette"
    :tabindex="apercu ? -1 : undefined"
    @click="$emit('click')"
    @pointerenter="$emit('survol')"
  >
    <span class="halo" aria-hidden="true" />
    <svg class="ico" viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M8 6V4.5a4 4 0 0 1 8 0V6M5 10H3m18 0h-2M5 15H3m18 0h-2M6.5 20l1.2-2m9.8 2-1.2-2M12 21a6 6 0 0 1-6-6v-4a5 5 0 0 1 12 0v4a6 6 0 0 1-6 6Zm0 0v-9"
        fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"
      />
    </svg>
    <span v-if="config.libelle" class="txt">{{ config.libelle }}</span>
    <Transition name="badge">
      <span v-if="badge" :key="badge" class="badge" aria-hidden="true">{{ badge }}</span>
    </Transition>
  </button>
</template>

<style scoped>
.kb {
  --pad: calc(var(--h) * 0.36);
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: calc(var(--h) * 0.18);
  height: var(--h);
  min-width: var(--h);
  padding: 0 var(--pad);
  border: 0;
  border-radius: 999px;
  background: var(--c);
  color: var(--ink);
  opacity: var(--o);
  font: 600 calc(var(--h) * 0.3) / 1 "Inter", system-ui, -apple-system, "Segoe UI", sans-serif;
  letter-spacing: 0;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 1px 2px rgb(0 0 0 / 0.25), 0 4px 12px -4px rgb(0 0 0 / 0.3);
  transition:
    background-color 0.45s ease,
    color 0.45s ease,
    opacity 0.45s ease,
    height 0.4s cubic-bezier(0.22, 1, 0.36, 1),
    padding 0.4s cubic-bezier(0.22, 1, 0.36, 1),
    scale 0.2s cubic-bezier(0.22, 1, 0.36, 1);
  -webkit-tap-highlight-color: transparent;
}
.kb.icone-seule { padding: 0; justify-content: center; width: var(--h); }
.kb:hover { scale: 1.03; opacity: max(var(--o), 0.85); }
.kb:active { scale: 0.97; }
.kb:focus-visible { outline: 2px solid #fff; outline-offset: 3px; box-shadow: 0 0 0 5px #1a1a18; }
.kb.apercu { cursor: default; }

.ico { width: calc(var(--h) * 0.46); height: calc(var(--h) * 0.46); flex: none; }

.halo {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  opacity: 0;
}

.badge {
  position: absolute;
  top: calc(var(--h) * -0.16);
  right: calc(var(--h) * -0.12);
  min-width: calc(var(--h) * 0.42);
  height: calc(var(--h) * 0.42);
  padding: 0 0.3em;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #1a1a18;
  color: #fff;
  font-size: max(11px, calc(var(--h) * 0.24));
  font-weight: 600;
  box-shadow: 0 0 0 2px #fff;
}
.badge-enter-active { transition: scale 0.45s var(--ease-spring, cubic-bezier(0.34, 1.56, 0.64, 1)), opacity 0.2s; }
.badge-leave-active { transition: scale 0.15s, opacity 0.15s; }
.badge-enter-from, .badge-leave-to { scale: 0; opacity: 0; }

/* ── Animations des variantes (désactivées si l'utilisateur réduit les animations) ── */
@media (prefers-reduced-motion: no-preference) {
  .anim-pulsation .halo {
    box-shadow: 0 0 0 0 var(--c);
    animation: pulsation 1.8s cubic-bezier(0.22, 1, 0.36, 1) infinite;
  }
  .anim-rebond { animation: rebond 2.4s cubic-bezier(0.28, 0.84, 0.42, 1) infinite; }
  .anim-secousse { animation: secousse 3.2s ease-in-out infinite; }
  .anim-lueur .halo { animation: lueur 2.2s ease-in-out infinite; }
  .anim-respiration { animation: respiration 3.6s ease-in-out infinite; }
}

@keyframes pulsation {
  0% { opacity: 0.9; box-shadow: 0 0 0 0 var(--c); }
  100% { opacity: 0; box-shadow: 0 0 0 calc(var(--h) * 0.45) var(--c); }
}
@keyframes rebond {
  0%, 60%, 100% { translate: 0 0; }
  20% { translate: 0 calc(var(--h) * -0.32); }
  35% { translate: 0 0; }
  45% { translate: 0 calc(var(--h) * -0.1); }
}
@keyframes secousse {
  0%, 70%, 100% { rotate: 0deg; }
  74% { rotate: -9deg; }
  78% { rotate: 8deg; }
  82% { rotate: -6deg; }
  86% { rotate: 4deg; }
  90% { rotate: 0deg; }
}
@keyframes lueur {
  0%, 100% { opacity: 0.25; box-shadow: 0 0 calc(var(--h) * 0.25) calc(var(--h) * 0.05) var(--c); }
  50% { opacity: 1; box-shadow: 0 0 calc(var(--h) * 0.6) calc(var(--h) * 0.18) var(--c); }
}
@keyframes respiration {
  0%, 100% { scale: 1; }
  50% { scale: 1.05; }
}
</style>
