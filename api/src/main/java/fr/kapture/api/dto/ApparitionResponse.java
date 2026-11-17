package fr.kapture.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.kapture.api.entity.Apparition;
import tools.jackson.databind.JsonNode;

/**
 * Une apparition telle que publiée dans la chronologie (détails de l'entrée
 * {@code APPARITION}).
 */
public record ApparitionResponse(

        @JsonProperty("id") Long id,

        @JsonProperty("session_id") Integer sessionId,

        @JsonProperty("titre") String titre,

        @JsonProperty("origine") String origine,

        @JsonProperty("commande_id") Long commandeId,

        @JsonProperty("affichage_id") Long affichageId,

        @JsonProperty("payload") JsonNode payload,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("timestamp") Instant timestamp

) {

    public static ApparitionResponse depuis(Apparition apparition) {
        return new ApparitionResponse(apparition.getId(), apparition.getSession().getId(),
                apparition.getTitre(), apparition.getOrigine(),
                apparition.getCommande() == null ? null : apparition.getCommande().getId(),
                apparition.getAffichage() == null ? null : apparition.getAffichage().getId(),
                apparition.getPayload(), apparition.getTimestamp());
    }
}
