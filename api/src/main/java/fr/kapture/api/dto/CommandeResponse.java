package fr.kapture.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.kapture.api.entity.Commande;
import tools.jackson.databind.JsonNode;

/**
 * Une commande ({@code Commande} du contrat v2) : réponse des routes de
 * commandes, donnée de l'événement SSE {@code commande} et détails de
 * l'entrée de chronologie {@code COMMANDE}.
 */
public record CommandeResponse(

        @JsonProperty("id") Long id,

        @JsonProperty("session_id") Integer sessionId,

        @JsonProperty("type") String type,

        @JsonProperty("parametres") JsonNode parametres,

        /** {@link StatutCommande}. */
        @JsonProperty("statut") String statut,

        @JsonProperty("message") String message,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("cree_a") Instant creeA,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("traite_a") Instant traiteA

) {

    public static CommandeResponse depuis(Commande commande) {
        return new CommandeResponse(commande.getId(), commande.getSession().getId(),
                commande.getType(), commande.getParametres(), commande.getStatut(),
                commande.getMessage(), commande.getCreeA(), commande.getTraiteA());
    }
}
