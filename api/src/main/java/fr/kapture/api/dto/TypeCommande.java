package fr.kapture.api.dto;

import java.util.Arrays;

/**
 * Type de commande envoyée au jeu (v2).
 */
public enum TypeCommande {

    DECLENCHER_BUG;

    /** Vrai si la valeur fait partie de la liste fermée. */
    public static boolean estValide(String valeur) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(valeur));
    }
}
