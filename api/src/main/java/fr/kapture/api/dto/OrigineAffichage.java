package fr.kapture.api.dto;

import java.util.Arrays;

/**
 * Origine d'un affichage (v2) : variante par défaut ({@code DEFAUT}) ou choisie par l'opérateur ({@code OPERATEUR}).
 */
public enum OrigineAffichage {

    DEFAUT,
    OPERATEUR;

    /** Vrai si la valeur fait partie de la liste fermée. */
    public static boolean estValide(String valeur) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(valeur));
    }
}
