package fr.kapture.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Une session et son état en direct ({@code GET /api/sessions},
 * réponse de {@code PUT /api/sessions/{id}/variante}).
 */
public record SessionEtat(

        @JsonProperty("id") Integer id,

        /** Pseudonyme du testeur, jamais le nominatif. */
        @JsonProperty("id_utilisateur") String idUtilisateur,

        @JsonProperty("application") String application,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("debut") Instant debut,

        /** Au moins un overlay connecté au flux SSE de la session. */
        @JsonProperty("connecte") boolean connecte,

        /** Nombre d'overlays connectés. */
        @JsonProperty("overlays") int overlays,

        @JsonProperty("variante") VarianteCourante variante

) {
}
