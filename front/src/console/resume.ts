import { titreLabel, typeCourt } from "@/shared/constants";
import type { EntreeChronologie } from "@/shared/types";

/** Phrase courte décrivant une entrée de chronologie (flux en direct, frise). */
export function resume(e: EntreeChronologie): { titre: string; detail: string; ton: string } {
  const d = e.details ?? {};
  switch (e.type) {
    case "AFFICHAGE":
      return {
        titre: `Variante ${d.code ?? "personnalisée"}`,
        detail: [d.origine === "DEFAUT" ? "par défaut" : "par l'opérateur", d.config?.visible === false ? "bouton masqué" : null]
          .filter(Boolean).join(" · "),
        ton: "accent",
      };
    case "COMMANDE": {
      const statut = d.statut as string | undefined;
      return {
        titre: `Déclencher ${d.parametres?.titre ?? "?"}`,
        detail: statut === "EXECUTEE" ? "exécutée par le jeu" : statut === "ECHEC" ? `échec : ${d.message ?? "?"}` : "envoyée au jeu",
        ton: statut === "ECHEC" ? "err" : statut === "EXECUTEE" ? "ok" : "warn",
      };
    }
    case "APPARITION":
      return {
        titre: `${d.titre} apparu`,
        detail: `${titreLabel(d.titre)} · ${d.origine === "COMMANDE" ? "déclenché par l'application" : "spontané"}`,
        ton: "warn",
      };
    case "INTERACTION": {
      const libelles: Record<string, string> = { SURVOL: "Survol du bouton", OUVERTURE: "Formulaire ouvert", ANNULATION: "Signalement abandonné" };
      return { titre: libelles[d.type] ?? d.type, detail: d.affichage_id ? `affichage n° ${d.affichage_id}` : "", ton: d.type === "ANNULATION" ? "err" : "muted" };
    }
    case "SIGNALEMENT":
      return {
        titre: `Signalement ${d.titre ?? ""}`,
        detail: [d.type_bug ? typeCourt(d.type_bug) : null, d.apparition_id ? `couvre l'apparition n° ${d.apparition_id}` : "sans apparition liée"]
          .filter(Boolean).join(" · "),
        ton: "ok",
      };
    case "CONNEXION":
      return {
        titre: d.connecte ? "Overlay connecté" : "Overlay déconnecté",
        detail: d.overlays != null ? `${d.overlays} overlay(s)` : "",
        ton: d.connecte ? "ok" : "muted",
      };
  }
}
