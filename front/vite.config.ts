import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// Trois pages : console (/), overlay en jeu (/overlay/), jeu de démo (/demo/).
// En dev, /api est relayé vers l'API locale (VITE_API=http://localhost:8090 pour viser un autre port).
export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  server: {
    port: 5173,
    proxy: { "/api": { target: process.env.VITE_API ?? "http://localhost:8080", changeOrigin: true } },
  },
  build: {
    rollupOptions: {
      input: {
        console: fileURLToPath(new URL("./index.html", import.meta.url)),
        overlay: fileURLToPath(new URL("./overlay/index.html", import.meta.url)),
        demo: fileURLToPath(new URL("./demo/index.html", import.meta.url)),
      },
    },
  },
});
