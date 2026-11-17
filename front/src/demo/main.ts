import { createApp } from "vue";
import "@/styles/console.css"; // jetons et composants du panneau opérateur (avant les styles du jeu)
import DemoGame from "./DemoGame.vue";

createApp(DemoGame).mount("#app");
