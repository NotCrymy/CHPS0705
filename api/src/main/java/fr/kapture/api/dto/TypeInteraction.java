package fr.kapture.api.dto;

import java.util.Arrays;

/**
 * Comportement du joueur face au bouton (v2).
 */
public enum TypeInteraction {

    SURVOL,
    OUVERTURE,
    ANNULATION;

    /** Vrai si la valeur fait partie de la liste fermée. */
    public static boolean estValide(String valeur) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(valeur));
    }
}
