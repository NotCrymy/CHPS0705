package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.kapture.api.entity.Variante;
import tools.jackson.databind.JsonNode;

/**
 * Une variante du catalogue ({@code GET /api/variantes}).
 */
public record VarianteResponse(

        @JsonProperty("id") Integer id,

        @JsonProperty("code") String code,

        @JsonProperty("nom") String nom,

        /** {@link Intention}. */
        @JsonProperty("intention") String intention,

        /** {@link ConfigBouton} sous forme JSON. */
        @JsonProperty("config") JsonNode config

) {

    public static VarianteResponse depuis(Variante variante) {
        return new VarianteResponse(variante.getId(), variante.getCode(), variante.getNom(),
                variante.getIntention(), variante.getConfig());
    }
}
