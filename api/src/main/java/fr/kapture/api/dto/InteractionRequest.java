package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.JsonNode;

/**
 * Corps de {@code POST /api/sessions/{id}/interactions} : survol, ouverture
 * ou annulation du bouton. Le service contrôle le {@code type}
 * ({@link TypeInteraction}), {@code affichage_id}, la forme de
 * {@code details} et le parsage du {@code timestamp}.
 */
public record InteractionRequest(

        @NotBlank(message = "Le type d'interaction est obligatoire.")
        @JsonProperty("type") String type,

        @JsonProperty("affichage_id") Long affichageId,

        /** Objet JSON libre, optionnel. */
        @JsonProperty("details") JsonNode details,

        @NotBlank(message = "Le timestamp est obligatoire.")
        @JsonProperty("timestamp") String timestamp

) {
}
