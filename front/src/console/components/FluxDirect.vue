<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue";
import { ENTREES } from "@/shared/constants";
import { heure, ilYa } from "@/shared/format";
import type { EntreeChronologie } from "@/shared/types";
import { resume } from "../resume";

defineProps<{ entrees: EntreeChronologie[]; afficherSession?: boolean; vide?: string }>();

// Horloge pour rafraîchir les « il y a … ».
const maintenant = ref(Date.now());
const minuteur = window.setInterval(() => (maintenant.value = Date.now()), 5000);
onBeforeUnmount(() => clearInterval(minuteur));

const cle = (e: EntreeChronologie) => `${e.type}-${e.id ?? e.timestamp}-${e.details?.statut ?? ""}`;
</script>

<template>
  <div class="flux">
    <p v-if="!entrees.length" class="vide">{{ vide ?? "Rien pour l'instant. Les événements apparaîtront ici en direct." }}</p>
    <TransitionGroup v-else tag="ol" name="list" class="liste">
      <li v-for="e in entrees" :key="cle(e)" class="ev" :class="`t-${resume(e).ton}`">
        <span class="ico" aria-hidden="true">{{ ENTREES[e.type].icone }}</span>
        <div class="corps">
          <div class="l1">
            <b>{{ resume(e).titre }}</b>
            <span v-if="afficherSession" class="chip">S{{ e.session_id }}</span>
          </div>
          <div v-if="resume(e).detail" class="l2">{{ resume(e).detail }}</div>
        </div>
        <time :datetime="e.timestamp" :title="heure(e.timestamp)">{{ ilYa(e.timestamp, maintenant) }}</time>
      </li>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.flux { position: relative; }
.vide { margin: 0; padding: 1.5rem 0.5rem; text-align: center; color: var(--muted); font-size: 0.9rem; }
.liste { list-style: none; margin: 0; padding: 0; display: grid; position: relative; }
.ev {
  display: grid; grid-template-columns: 22px 1fr auto; gap: 0.55rem; align-items: start;
  padding: 0.55rem 0.15rem; width: 100%;
  border-bottom: 1px solid var(--line);
}
.ev:last-child { border-bottom: 0; }
.ico {
  width: 22px; height: 22px; border-radius: 4px; display: grid; place-items: center; font-size: 0.75rem;
  background: var(--surface-3); color: var(--ink-2);
}
.t-accent .ico { background: var(--accent-soft); color: var(--accent); }
.t-ok .ico { background: var(--ok-soft); color: var(--ok); }
.t-warn .ico { background: var(--warn-soft); color: var(--warn); }
.t-err .ico { background: var(--err-soft); color: var(--err); }
.corps { min-width: 0; }
.l1 { display: flex; gap: 0.4rem; align-items: center; }
.l1 b { font-weight: 600; }
.l2 { font-size: 0.8125rem; color: var(--muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
time { font-family: var(--mono); font-size: 0.75rem; color: var(--muted); white-space: nowrap; padding-top: 0.1rem; }
</style>
