import type { Animation, ConfigBouton, Intention, Position, Taille, TypeEntree } from "./types";

/* Valeurs de la spec (Specs.md). Les titres ne sont pas imposés par l'API : convention du POC. */

export const TITRES = [
  { value: "BUG1", label: "Traverse les murs", nature: "avantageux" },
  { value: "BUG2", label: "Perte d'une vie sans raison", nature: "désavantageux" },
  { value: "BUG3", label: "Score doublé 30 s", nature: "avantageux" },
  { value: "BUG4", label: "Commandes inversées 10 s", nature: "désavantageux" },
  { value: "BUG5", label: "Texture manquante", nature: "neutre" },
] as const;

export const titreLabel = (v: string) => TITRES.find((t) => t.value === v)?.label ?? v;

export const TYPES_BUG = [
  { value: "MAP", label: "Décor, collision, zone inaccessible", court: "Décor" },
  { value: "AUTRE_JOUEUR", label: "Autre joueur", court: "Autre joueur" },
  { value: "GRAPHIQUE", label: "Affichage, textures, animations", court: "Graphique" },
  { value: "SON", label: "Son absent, décalé ou en boucle", court: "Son" },
  { value: "INTERFACE", label: "Menus, HUD, boutons", court: "Interface" },
  { value: "PERFORMANCE", label: "Ralentissement, gel, plantage", court: "Performance" },
  { value: "AUTRE", label: "Autre", court: "Autre" },
] as const;

export const typeCourt = (v: string) => TYPES_BUG.find((t) => t.value === v)?.court ?? v;

export const TAILLES: { value: Taille; label: string }[] = [
  { value: "S", label: "Petit" },
  { value: "M", label: "Moyen" },
  { value: "L", label: "Grand" },
  { value: "XL", label: "Très grand" },
];

export const ANIMATIONS: { value: Animation; label: string }[] = [
  { value: "aucune", label: "Aucune" },
  { value: "respiration", label: "Respiration" },
  { value: "pulsation", label: "Pulsation" },
  { value: "rebond", label: "Rebond" },
  { value: "secousse", label: "Secousse" },
  { value: "lueur", label: "Lueur" },
];

export const POSITIONS: { value: Position; label: string }[] = [
  { value: "haut-gauche", label: "Haut gauche" },
  { value: "haut-droite", label: "Haut droite" },
  { value: "bas-gauche", label: "Bas gauche" },
  { value: "bas-droite", label: "Bas droite" },
];

export const INTENTIONS: Record<Intention, { label: string; ton: string }> = {
  INCITER: { label: "Incite", ton: "inciter" },
  NEUTRE: { label: "Neutre", ton: "neutre" },
  DISSUADER: { label: "Dissuade", ton: "dissuader" },
};

export const ENTREES: Record<TypeEntree, { label: string; icone: string }> = {
  AFFICHAGE: { label: "Variante affichée", icone: "◐" },
  COMMANDE: { label: "Commande", icone: "⌁" },
  APPARITION: { label: "Bug apparu", icone: "✦" },
  INTERACTION: { label: "Interaction", icone: "◎" },
  SIGNALEMENT: { label: "Signalement", icone: "⚑" },
  CONNEXION: { label: "Connexion", icone: "●" },
};

/** Variante NEUTRE (secours si l'API ne répond pas encore). */
export const CONFIG_NEUTRE: ConfigBouton = {
  visible: true,
  couleur: "#64748b",
  taille: "M",
  opacite: 1,
  libelle: "Signaler",
  badge: { mode: "aucun", texte: "" },
  animation: "aucune",
  position: "bas-droite",
};
