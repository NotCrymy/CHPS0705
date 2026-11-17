package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * Corps de {@code POST /api/sessions/{id}/apparitions} : le jeu (via
 * l'overlay) signale un bug apparu. Le service contrôle {@code origine}
 * ({@link OrigineApparition}), l'existence et la session de
 * {@code commande_id} / {@code affichage_id}, la forme du {@code payload} et
 * le parsage du {@code timestamp}.
 */
public record ApparitionRequest(

        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 100, message = "Le titre ne doit pas dépasser 100 caractères.")
        @JsonProperty("titre") String titre,

        @NotBlank(message = "L'origine est obligatoire.")
        @JsonProperty("origine") String origine,

        @JsonProperty("commande_id") Long commandeId,

        @JsonProperty("affichage_id") Long affichageId,

        /** État du jeu, objet JSON optionnel. */
        @JsonProperty("payload") JsonNode payload,

        @NotBlank(message = "Le timestamp est obligatoire.")
        @JsonProperty("timestamp") String timestamp

) {
}
