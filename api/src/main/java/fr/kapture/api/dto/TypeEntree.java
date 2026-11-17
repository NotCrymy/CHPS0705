package fr.kapture.api.dto;

import java.util.Arrays;

/**
 * Type d'une entrée de chronologie (v2, {@code EntreeChronologie.type}).
 */
public enum TypeEntree {

    AFFICHAGE,
    COMMANDE,
    APPARITION,
    INTERACTION,
    SIGNALEMENT,
    CONNEXION;

    /** Vrai si la valeur fait partie de la liste fermée. */
    public static boolean estValide(String valeur) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(valeur));
    }
}
