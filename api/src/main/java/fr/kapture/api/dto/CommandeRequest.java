package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

/**
 * Corps de {@code POST /api/sessions/{id}/commandes} :
 * {@code { "type": "DECLENCHER_BUG", "parametres": { "titre": "BUG3" } }}.
 *
 * <p>Pas de contrainte Jakarta : les messages du contrat (« Type de commande
 * inconnu. », « Le titre du bug est obligatoire. ») dépendent du type et du
 * contenu de {@code parametres}, ils sont produits par le service.</p>
 */
public record CommandeRequest(

        /** {@link TypeCommande}. */
        @JsonProperty("type") String type,

        /** Objet JSON ; pour {@code DECLENCHER_BUG} : {@code titre} obligatoire. */
        @JsonProperty("parametres") JsonNode parametres

) {
}
