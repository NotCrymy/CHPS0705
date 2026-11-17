package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Apparence du bouton de signalement de l'overlay ({@code docs/V2.md} §5.1).
 *
 * <p>Toutes les règles sont des contraintes Jakarta, avec les messages exacts
 * du contrat ; elles sont déclenchées par {@code @Valid} sur le corps qui
 * contient la config ({@code PUT …/variante}, {@code POST …/affichages}). Une
 * valeur absente reçoit le message de la règle correspondante. La config
 * validée est stockée telle quelle en {@code JsonNode} (colonnes
 * {@code JSON}).</p>
 */
public record ConfigBouton(

        @NotNull(message = "La visibilité est obligatoire.")
        @JsonProperty("visible") Boolean visible,

        /** {@code #rrggbb}. */
        @NotNull(message = "Couleur invalide (format #rrggbb).")
        @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Couleur invalide (format #rrggbb).")
        @JsonProperty("couleur") String couleur,

        @NotNull(message = "Taille invalide.")
        @Pattern(regexp = "S|M|L|XL", message = "Taille invalide.")
        @JsonProperty("taille") String taille,

        @NotNull(message = "Opacité invalide (entre 0.1 et 1).")
        @DecimalMin(value = "0.1", message = "Opacité invalide (entre 0.1 et 1).")
        @DecimalMax(value = "1", message = "Opacité invalide (entre 0.1 et 1).")
        @JsonProperty("opacite") Double opacite,

        /** 0 à 40 caractères ; {@code ""} (ou absent) = icône seule. */
        @Size(max = 40, message = "Le libellé ne doit pas dépasser 40 caractères.")
        @JsonProperty("libelle") String libelle,

        @NotNull(message = "Badge invalide.")
        @Valid
        @JsonProperty("badge") Badge badge,

        @NotNull(message = "Animation invalide.")
        @Pattern(regexp = "aucune|pulsation|rebond|secousse|lueur|respiration", message = "Animation invalide.")
        @JsonProperty("animation") String animation,

        @NotNull(message = "Position invalide.")
        @Pattern(regexp = "bas-droite|bas-gauche|haut-droite|haut-gauche", message = "Position invalide.")
        @JsonProperty("position") String position

) {

    /** Un libellé absent est stocké comme {@code ""} (icône seule). */
    public ConfigBouton {
        if (libelle == null) {
            libelle = "";
        }
    }

    /**
     * Pastille du bouton : {@code aucun}, {@code texte} (jusqu'à 3
     * caractères) ou {@code compteur} (apparitions non signalées, calculé par
     * l'overlay).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Badge(

            @NotNull(message = "Badge invalide.")
            @Pattern(regexp = "aucun|texte|compteur", message = "Badge invalide.")
            @JsonProperty("mode") String mode,

            @Size(max = 3, message = "Le texte du badge ne doit pas dépasser 3 caractères.")
            @JsonProperty("texte") String texte

    ) {
    }
}
