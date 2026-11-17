package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/sessions/{id}/affichages} : l'overlay confirme
 * qu'il affiche une variante. Le service contrôle {@code variante_id}
 * (« Variante inconnue. »), {@code origine} ({@link OrigineAffichage}) et le
 * parsage du {@code timestamp}.
 */
public record AffichageRequest(

        /** Variante du catalogue, {@code null} = configuration personnalisée. */
        @JsonProperty("variante_id") Integer varianteId,

        /** Copie exacte de ce qui est rendu. */
        @NotNull(message = "La configuration est obligatoire.")
        @Valid
        @JsonProperty("config") ConfigBouton config,

        @NotBlank(message = "L'origine est obligatoire.")
        @JsonProperty("origine") String origine,

        /** Début de l'affichage, ISO 8601. */
        @NotBlank(message = "Le timestamp est obligatoire.")
        @JsonProperty("timestamp") String timestamp

) {
}
