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
 * Table {@code affichage} : une variante effectivement rendue par l'overlay,
 * avec sa période d'affichage ({@code debut} → {@code fin}).
 *
 * <p>{@code fin} est renseignée quand l'overlay confirme un nouvel affichage
 * (fin = début du suivant) ou quand le dernier overlay de la session se
 * déconnecte (fin = maintenant). {@code variante} est {@code null} pour une
 * configuration personnalisée.</p>
 */
@Entity
@Table(name = "affichage")
public class Affichage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** Variante du catalogue, {@code null} = configuration personnalisée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id")
    private Variante variante;

    /** Copie exacte de la {@code ConfigBouton} rendue. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false)
    private JsonNode config;

    /** {@code DEFAUT} ou {@code OPERATEUR}. */
    @Column(name = "origine", length = 20, nullable = false)
    private String origine;

    @Column(name = "debut", nullable = false)
    private Instant debut;

    /** {@code null} tant que l'affichage est en cours. */
    @Column(name = "fin")
    private Instant fin;

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

    public Variante getVariante() {
        return variante;
    }

    public void setVariante(Variante variante) {
        this.variante = variante;
    }

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public Instant getDebut() {
        return debut;
    }

    public void setDebut(Instant debut) {
        this.debut = debut;
    }

    public Instant getFin() {
        return fin;
    }

    public void setFin(Instant fin) {
        this.fin = fin;
    }
}
