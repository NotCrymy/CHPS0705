package fr.kapture.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.kapture.api.entity.Affichage;
import tools.jackson.databind.JsonNode;

/**
 * Un affichage tel que publié dans la chronologie (détails de l'entrée
 * {@code AFFICHAGE}). {@code variante_id} / {@code code} nuls pour une
 * configuration personnalisée ; {@code fin} nulle tant qu'il est en cours.
 */
public record AffichageResponse(

        @JsonProperty("id") Long id,

        @JsonProperty("session_id") Integer sessionId,

        @JsonProperty("variante_id") Integer varianteId,

        @JsonProperty("code") String code,

        @JsonProperty("config") JsonNode config,

        @JsonProperty("origine") String origine,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("debut") Instant debut,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("fin") Instant fin

) {

    public static AffichageResponse depuis(Affichage affichage) {
        boolean personnalisee = affichage.getVariante() == null;
        return new AffichageResponse(affichage.getId(), affichage.getSession().getId(),
                personnalisee ? null : affichage.getVariante().getId(),
                personnalisee ? null : affichage.getVariante().getCode(),
                affichage.getConfig(), affichage.getOrigine(), affichage.getDebut(), affichage.getFin());
    }
}
