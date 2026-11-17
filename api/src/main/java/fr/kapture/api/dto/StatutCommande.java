package fr.kapture.api.dto;

import java.util.Arrays;

/**
 * Cycle de vie d'une commande (v2) : {@code ENVOYEE} puis {@code EXECUTEE} ou {@code ECHEC}.
 */
public enum StatutCommande {

    ENVOYEE,
    EXECUTEE,
    ECHEC;

    /** Vrai si la valeur fait partie de la liste fermée. */
    public static boolean estValide(String valeur) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(valeur));
    }
}
