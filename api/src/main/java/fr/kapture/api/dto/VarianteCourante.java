package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

/**
 * Variante en vigueur pour une session ({@code SessionEtat.variante}, et
 * donnée de l'événement SSE {@code variante} envoyé aux overlays).
 *
 * <p>{@code variante_id} et {@code code} valent {@code null} pour une
 * configuration personnalisée. Sans assignation : {@code NEUTRE} avec
 * l'origine {@code DEFAUT}.</p>
 */
public record VarianteCourante(

        @JsonProperty("variante_id") Integer varianteId,

        @JsonProperty("code") String code,

        /** {@link OrigineAffichage}. */
        @JsonProperty("origine") String origine,

        @JsonProperty("config") JsonNode config

) {
}
