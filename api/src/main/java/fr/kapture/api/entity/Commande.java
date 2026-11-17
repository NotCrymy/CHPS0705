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
 * Table {@code commande} : ordre envoyé au jeu par l'application (via
 * l'overlay), ex. déclencher un bug. Cycle de vie : {@code ENVOYEE} puis
 * {@code EXECUTEE} ou {@code ECHEC} quand l'overlay rend le résultat.
 */
@Entity
@Table(name = "commande")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** {@code DECLENCHER_BUG}. */
    @Column(name = "type", length = 30, nullable = false)
    private String type;

    /** Ex. {@code { "titre": "BUG3" }}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parametres", nullable = false)
    private JsonNode parametres;

    /** {@code ENVOYEE}, {@code EXECUTEE} ou {@code ECHEC}. */
    @Column(name = "statut", length = 20, nullable = false)
    private String statut;

    /** Raison d'un échec, éventuellement. */
    @Column(name = "message", length = 200)
    private String message;

    @Column(name = "cree_a", nullable = false)
    private Instant creeA;

    @Column(name = "traite_a")
    private Instant traiteA;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonNode getParametres() {
        return parametres;
    }

    public void setParametres(JsonNode parametres) {
        this.parametres = parametres;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreeA() {
        return creeA;
    }

    public void setCreeA(Instant creeA) {
        this.creeA = creeA;
    }

    public Instant getTraiteA() {
        return traiteA;
    }

    public void setTraiteA(Instant traiteA) {
        this.traiteA = traiteA;
    }
}
