package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/commandes/{id}/resultat} :
 * {@code { "succes": true, "message": null }} → statut {@code EXECUTEE} ou
 * {@code ECHEC}.
 */
public record ResultatCommandeRequest(

        @NotNull(message = "Le résultat (succes) est obligatoire.")
        @JsonProperty("succes") Boolean succes,

        /** Raison d'un échec, optionnelle. */
        @Size(max = 200, message = "Le message ne doit pas dépasser 200 caractères.")
        @JsonProperty("message") String message

) {
}
