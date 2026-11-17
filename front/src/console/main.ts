import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";
import App from "./App.vue";
import "@/styles/console.css";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: () => import("./views/PilotageView.vue"), meta: { titre: "Pilotage" } },
    { path: "/suivi", component: () => import("./views/SuiviView.vue"), meta: { titre: "Suivi" } },
    { path: "/signalements", component: () => import("./views/BugsView.vue"), meta: { titre: "Signalements" } },
  ],
});
router.afterEach((to) => { document.title = `${to.meta.titre ?? "Console"} — Kapture Fairplay`; });

createApp(App).use(router).mount("#app");
