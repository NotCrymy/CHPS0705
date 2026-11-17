<script setup lang="ts">
import { computed, onMounted } from "vue";
import { demarrer, enDirect, sessions } from "./live";

onMounted(demarrer);
const connectes = computed(() => sessions.value.filter((s) => s.connecte).length);
</script>

<template>
  <div class="shell">
    <header class="top">
      <RouterLink to="/" class="brand">
        <span class="logo" aria-hidden="true">K</span>
        <span><b>Kapture</b> <span class="sous">Fairplay</span></span>
      </RouterLink>
      <nav aria-label="Sections">
        <RouterLink to="/">Pilotage</RouterLink>
        <RouterLink to="/suivi">Suivi</RouterLink>
        <RouterLink to="/signalements">Signalements</RouterLink>
        <a href="/demo/?session=42&user=Storm_X9" target="_blank" rel="noopener">Jeu de démo</a>
      </nav>
      <div class="live" :class="{ on: enDirect }" role="status">
        <span class="dot" :class="{ on: enDirect }" />
        <span>{{ enDirect ? "En direct" : "Hors ligne" }}</span>
        <Transition name="fade" mode="out-in">
          <span :key="connectes" class="n">{{ connectes }} overlay{{ connectes > 1 ? "s" : "" }}</span>
        </Transition>
      </div>
    </header>

    <main class="page">
      <RouterView v-slot="{ Component, route }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="route.path" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<style scoped>
.top {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: stretch; gap: 2rem;
  min-height: 56px; padding: 0 1.5rem;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.brand { display: flex; align-items: center; gap: 0.6rem; color: var(--ink); text-decoration: none; letter-spacing: -0.01em; }
.brand b { font-weight: 700; }
.sous { color: var(--muted); }
.logo {
  width: 22px; height: 22px; border-radius: 5px; display: grid; place-items: center;
  background: var(--ink); color: #fff; font-size: 0.75rem; font-weight: 700;
}
nav { display: flex; gap: 0.25rem; flex: 1; overflow-x: auto; }
nav a {
  display: flex; align-items: center; padding: 0 0.65rem;
  color: var(--ink-2); text-decoration: none; font-weight: 500; white-space: nowrap;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  transition: color 0.15s, border-color 0.15s;
}
nav a:hover { color: var(--ink); }
nav a.router-link-exact-active { color: var(--ink); font-weight: 600; border-bottom-color: var(--ink); }
.live { display: flex; align-items: center; gap: 0.5rem; font-size: 0.8125rem; color: var(--muted); white-space: nowrap; }
.live.on { color: var(--ink-2); }
.n::before { content: "· "; }
.page { padding: 1.5rem; max-width: 1480px; margin: 0 auto; }

.page-enter-active { transition: opacity 0.2s var(--ease-out); }
.page-leave-active { transition: opacity 0.1s; }
.page-enter-from, .page-leave-to { opacity: 0; }

@media (max-width: 720px) {
  .top { flex-wrap: wrap; gap: 0 1rem; padding: 0.5rem 1rem 0; }
  .brand, .live { min-height: 40px; }
  nav { order: 3; flex-basis: 100%; }
  nav a { min-height: 40px; }
  .page { padding: 1rem; }
}
</style>
