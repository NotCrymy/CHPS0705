/* Types du contrat API (docs/V2.md). Clés en français snake_case, comme l'API. */

export type Taille = "S" | "M" | "L" | "XL";
export type ModeBadge = "aucun" | "texte" | "compteur";
export type Animation = "aucune" | "pulsation" | "rebond" | "secousse" | "lueur" | "respiration";
export type Position = "bas-droite" | "bas-gauche" | "haut-droite" | "haut-gauche";
export type Intention = "INCITER" | "NEUTRE" | "DISSUADER";
export type OrigineAffichage = "DEFAUT" | "OPERATEUR";
export type OrigineApparition = "JEU" | "COMMANDE";
export type TypeInteraction = "SURVOL" | "OUVERTURE" | "ANNULATION";
export type StatutCommande = "ENVOYEE" | "EXECUTEE" | "ECHEC";

export interface ConfigBouton {
  visible: boolean;
  couleur: string;
  taille: Taille;
  opacite: number;
  libelle: string;
  badge: { mode: ModeBadge; texte: string };
  animation: Animation;
  position: Position;
}

export interface Variante {
  id: number;
  code: string;
  nom: string;
  intention: Intention;
  config: ConfigBouton;
}

export interface VarianteCourante {
  variante_id: number | null;
  code: string | null;
  origine: OrigineAffichage;
  config: ConfigBouton;
}

export interface SessionEtat {
  id: number;
  id_utilisateur: string;
  application: string;
  debut: string;
  connecte: boolean;
  overlays: number;
  variante: VarianteCourante | null;
}

export interface Commande {
  id: number;
  session_id: number;
  type: "DECLENCHER_BUG";
  parametres: { titre: string };
  statut: StatutCommande;
  message: string | null;
  cree_a: string;
  traite_a: string | null;
}

export interface Bug {
  id: number;
  session_id: number;
  titre: string;
  id_utilisateur: string;
  description: string;
  payload: Record<string, unknown> | null;
  timestamp: string;
  type_bug: string;
  affichage_id?: number | null;
  apparition_id?: number | null;
}

export interface Page<T> {
  total: number;
  page: number;
  taille: number;
  resultats: T[];
}

export type TypeEntree = "AFFICHAGE" | "COMMANDE" | "APPARITION" | "INTERACTION" | "SIGNALEMENT" | "CONNEXION";

export interface EntreeChronologie {
  type: TypeEntree;
  id: number | null;
  session_id: number;
  timestamp: string;
  details: Record<string, any>;
}

export interface StatVariante {
  variante_id: number | null;
  code: string;
  nom: string;
  intention: Intention | null;
  affichages: number;
  duree_ms: number;
  apparitions: number;
  signalements: number;
  signalements_couvrant_apparition: number;
  survols: number;
  ouvertures: number;
  annulations: number;
  taux_signalement: number | null;
}
