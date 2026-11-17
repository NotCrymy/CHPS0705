import { reactive, ref } from "vue";
import { api } from "@/shared/api";
import { ouvrirFlux } from "@/shared/sse";
import type { EntreeChronologie, SessionEtat, Variante } from "@/shared/types";

/**
 * État partagé de la console : flux global /api/flux, sessions et catalogue.
 * Ouvert une seule fois pour toute l'application.
 */

export const enDirect = ref(false);
export const entrees = ref<EntreeChronologie[]>([]);
export const sessions = ref<SessionEtat[]>([]);
export const variantes = ref<Variante[]>([]);
export const erreurs = reactive({ sessions: "", variantes: "" });

const ecouteurs = new Set<(e: EntreeChronologie) => void>();
export function surEntree(f: (e: EntreeChronologie) => void) {
  ecouteurs.add(f);
  return () => ecouteurs.delete(f);
}

let minuteurSessions = 0;
function rechargerSessionsBientot() {
  clearTimeout(minuteurSessions);
  minuteurSessions = window.setTimeout(chargerSessions, 250);
}

export async function chargerSessions() {
  try {
    sessions.value = await api.sessions();
    erreurs.sessions = "";
  } catch (e) {
    erreurs.sessions = (e as Error).message;
  }
}

export async function chargerVariantes() {
  try {
    variantes.value = await api.variantes();
    erreurs.variantes = "";
  } catch (e) {
    erreurs.variantes = (e as Error).message;
  }
}

let demarre = false;
export function demarrer() {
  if (demarre) return;
  demarre = true;
  chargerSessions();
  chargerVariantes();
  ouvrirFlux("/api/flux", {
    chronologie: (e: EntreeChronologie) => {
      entrees.value.unshift(e);
      if (entrees.value.length > 400) entrees.value.length = 400;
      if (e.type === "CONNEXION" || e.type === "AFFICHAGE") rechargerSessionsBientot();
      ecouteurs.forEach((f) => f(e));
    },
  }, enDirect);
  // Les connexions de l'EventSource après une coupure : on resynchronise les sessions.
  window.setInterval(() => enDirect.value || chargerSessions(), 10000);
}
