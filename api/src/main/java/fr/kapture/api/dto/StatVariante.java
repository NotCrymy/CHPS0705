package fr.kapture.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Statistiques d'une variante ({@code GET /api/suivi/variantes}).
 *
 * <p>Chaque comportement est attribué à la variante de l'affichage qu'il
 * porte ({@code affichage_id}) ; ce qui n'a pas d'{@code affichage_id}
 * n'est compté nulle part. Les configurations personnalisées sont regroupées
 * sous {@code variante_id = null}, {@code code = "PERSONNALISEE"}.</p>
 */
public record StatVariante(

        @JsonProperty("variante_id") Integer varianteId,

        @JsonProperty("code") String code,

        @JsonProperty("nom") String nom,

        @JsonProperty("intention") String intention,

        /** Nombre d'affichages. */
        @JsonProperty("affichages") long affichages,

        /** Durée cumulée des affichages ; un affichage ouvert compte jusqu'à maintenant. */
        @JsonProperty("duree_ms") long dureeMs,

        @JsonProperty("apparitions") long apparitions,

        @JsonProperty("signalements") long signalements,

        /** Apparitions distinctes couvertes par au moins un signalement ({@code apparition_id}). */
        @JsonProperty("signalements_couvrant_apparition") long signalementsCouvrantApparition,

        @JsonProperty("survols") long survols,

        @JsonProperty("ouvertures") long ouvertures,

        @JsonProperty("annulations") long annulations,

        /** {@code signalements_couvrant_apparition / apparitions}, {@code null} si aucune apparition. */
        @JsonProperty("taux_signalement") Double tauxSignalement

) {
}
