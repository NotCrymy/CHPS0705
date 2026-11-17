package fr.kapture.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.kapture.api.entity.Affichage;
import fr.kapture.api.entity.Apparition;
import fr.kapture.api.entity.Commande;
import fr.kapture.api.entity.Evenement;
import fr.kapture.api.entity.Interaction;

/**
 * Une entrée de chronologie ({@code GET /api/sessions/{id}/chronologie} et
 * événement SSE {@code chronologie} de {@code /api/flux}).
 *
 * <p>{@code details} porte l'objet concerné (même forme que sa réponse REST).
 * {@code timestamp} : {@code debut} pour un affichage, {@code cree_a} pour une
 * commande (ou {@code traite_a} quand son résultat est publié), le
 * {@code timestamp} fourni par le client sinon. Les entrées {@code CONNEXION}
 * ne sont pas persistées : {@code id} nul, publiées seulement sur le flux.</p>
 */
public record EntreeChronologie(

        /** {@link TypeEntree}. */
        @JsonProperty("type") String type,

        @JsonProperty("id") Long id,

        @JsonProperty("session_id") Integer sessionId,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("timestamp") Instant timestamp,

        @JsonProperty("details") Object details

) {

    public static EntreeChronologie affichage(Affichage affichage) {
        return new EntreeChronologie(TypeEntree.AFFICHAGE.name(), affichage.getId(),
                affichage.getSession().getId(), affichage.getDebut(), AffichageResponse.depuis(affichage));
    }

    /** Commande à sa création ({@code cree_a}). */
    public static EntreeChronologie commande(Commande commande) {
        return new EntreeChronologie(TypeEntree.COMMANDE.name(), commande.getId(),
                commande.getSession().getId(), commande.getCreeA(), CommandeResponse.depuis(commande));
    }

    /** Résultat d'une commande ({@code traite_a}) : même id, statut à jour. */
    public static EntreeChronologie resultatCommande(Commande commande) {
        return new EntreeChronologie(TypeEntree.COMMANDE.name(), commande.getId(),
                commande.getSession().getId(), commande.getTraiteA(), CommandeResponse.depuis(commande));
    }

    public static EntreeChronologie apparition(Apparition apparition) {
        return new EntreeChronologie(TypeEntree.APPARITION.name(), apparition.getId(),
                apparition.getSession().getId(), apparition.getTimestamp(), ApparitionResponse.depuis(apparition));
    }

    public static EntreeChronologie interaction(Interaction interaction) {
        return new EntreeChronologie(TypeEntree.INTERACTION.name(), interaction.getId(),
                interaction.getSession().getId(), interaction.getTimestamp(), InteractionResponse.depuis(interaction));
    }

    public static EntreeChronologie signalement(Evenement evenement) {
        return new EntreeChronologie(TypeEntree.SIGNALEMENT.name(), evenement.getId(),
                evenement.getSession().getId(), evenement.getTimestamp(), BugResponse.depuis(evenement));
    }

    /** (Dé)connexion d'un overlay : {@code details = { connecte, overlays }}. */
    public static EntreeChronologie connexion(Integer sessionId, int overlays, Instant maintenant) {
        return new EntreeChronologie(TypeEntree.CONNEXION.name(), null, sessionId, maintenant,
                new EtatConnexion(overlays > 0, overlays));
    }

    /** Détails d'une entrée {@code CONNEXION}. */
    public record EtatConnexion(
            @JsonProperty("connecte") boolean connecte,
            @JsonProperty("overlays") int overlays) {
    }
}
