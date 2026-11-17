package fr.kapture.api.dto;

import java.util.Arrays;

/**
 * Intention d'une variante du bouton (v2) : pousser le joueur à signaler, rester neutre ou le dissuader.
 */
public enum Intention {

    INCITER,
    NEUTRE,
    DISSUADER;

    /** Vrai si la valeur fait partie de la liste fermée. */
    public static boolean estValide(String valeur) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(valeur));
    }
}
