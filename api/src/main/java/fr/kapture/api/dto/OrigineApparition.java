package fr.kapture.api.dto;

import java.util.Arrays;

/**
 * Origine d'une apparition de bug (v2) : spontanée dans le jeu ({@code JEU}) ou déclenchée par une commande de l'application ({@code COMMANDE}).
 */
public enum OrigineApparition {

    JEU,
    COMMANDE;

    /** Vrai si la valeur fait partie de la liste fermée. */
    public static boolean estValide(String valeur) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(valeur));
    }
}
