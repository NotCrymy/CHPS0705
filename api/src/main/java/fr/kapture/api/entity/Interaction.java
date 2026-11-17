package fr.kapture.api.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import tools.jackson.databind.JsonNode;

/**
 * Table {@code interaction} : comportement du joueur face au bouton
 * ({@code SURVOL}, {@code OUVERTURE}, {@code ANNULATION}), relié à
 * l'affichage en cours.
 */
@Entity
@Table(name = "interaction")
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affichage_id")
    private Affichage affichage;

    /** {@code SURVOL}, {@code OUVERTURE} ou {@code ANNULATION}. */
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    /** Détails libres (durée du survol…), stockés tels quels. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details")
    private JsonNode details;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Affichage getAffichage() {
        return affichage;
    }

    public void setAffichage(Affichage affichage) {
        this.affichage = affichage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonNode getDetails() {
        return details;
    }

    public void setDetails(JsonNode details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
