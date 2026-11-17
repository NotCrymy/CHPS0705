package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;

/**
 * Corps de {@code PUT /api/sessions/{id}/variante} : l'un <em>ou</em>
 * l'autre champ (contrôlé par le service : « Indiquer variante_id ou
 * config. »). La config éventuelle est validée par Jakarta ({@link ConfigBouton}).
 */
public record AssignationVarianteRequest(

        /** Variante du catalogue. */
        @JsonProperty("variante_id") Integer varianteId,

        /** Configuration personnalisée. */
        @Valid
        @JsonProperty("config") ConfigBouton config

) {
}
