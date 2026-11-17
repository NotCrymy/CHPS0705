package fr.kapture.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.kapture.api.entity.Interaction;
import tools.jackson.databind.JsonNode;

/**
 * Une interaction telle que publiée dans la chronologie (détails de l'entrée
 * {@code INTERACTION}).
 */
public record InteractionResponse(

        @JsonProperty("id") Long id,

        @JsonProperty("session_id") Integer sessionId,

        @JsonProperty("affichage_id") Long affichageId,

        @JsonProperty("type") String type,

        @JsonProperty("details") JsonNode details,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("timestamp") Instant timestamp

) {

    public static InteractionResponse depuis(Interaction interaction) {
        return new InteractionResponse(interaction.getId(), interaction.getSession().getId(),
                interaction.getAffichage() == null ? null : interaction.getAffichage().getId(),
                interaction.getType(), interaction.getDetails(), interaction.getTimestamp());
    }
}
