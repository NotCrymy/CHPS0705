/**
 * Pont overlay ↔ jeu (postMessage). Protocole : docs/V2.md §4.
 * L'overlay ne connaît pas l'origine du jeu : il n'accepte que les messages de sa fenêtre parente.
 */

export interface MessageJeu {
  source: "kapture-game";
  type: "bug" | "trigger-result" | "state";
  [cle: string]: any;
}

type Ecouteur = (msg: MessageJeu) => void;

const parent = window.parent !== window ? window.parent : null;
const ecouteurs = new Set<Ecouteur>();

window.addEventListener("message", (e) => {
  const msg = e.data;
  if (!parent || e.source !== parent || !msg || msg.source !== "kapture-game") return;
  ecouteurs.forEach((f) => f(msg));
});

export const dansUnJeu = parent !== null;

export function envoyer(type: string, details: Record<string, unknown> = {}) {
  // "*" : l'origine du jeu est inconnue de l'overlay ; aucune donnée sensible ne transite.
  parent?.postMessage({ source: "kapture-overlay", type, ...details }, "*");
}

export function ecouter(f: Ecouteur) {
  ecouteurs.add(f);
  return () => ecouteurs.delete(f);
}

/** Attend la réponse du jeu qui satisfait `filtre`, ou rejette après `delai` ms. */
export function attendre(filtre: (m: MessageJeu) => boolean, delai: number): Promise<MessageJeu> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      stop();
      reject(new Error("Le jeu n'a pas répondu."));
    }, delai);
    const stop = ecouter((m) => {
      if (!filtre(m)) return;
      clearTimeout(timer);
      stop();
      resolve(m);
    });
  });
}

let compteurRequetes = 0;

/** État courant du jeu (payload), ou {} si le jeu ne répond pas. */
export async function etatDuJeu(): Promise<Record<string, unknown>> {
  if (!parent) return {};
  const request_id = ++compteurRequetes;
  envoyer("get-state", { request_id });
  try {
    const rep = await attendre((m) => m.type === "state" && m.request_id === request_id, 800);
    return objet(rep.payload);
  } catch {
    return {};
  }
}

export const objet = (v: unknown): Record<string, unknown> =>
  typeof v === "object" && v !== null && !Array.isArray(v) ? (v as Record<string, unknown>) : {};
