package fr.kapture.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import tools.jackson.databind.JsonNode;

/**
 * Table {@code variante} : catalogue des variantes du bouton de signalement
 * (v2, {@code docs/V2.md} §3 et §5.2).
 *
 * <p>Le catalogue est inséré au démarrage s'il est vide
 * ({@code fr.kapture.api.config.InitialisationCatalogue}).</p>
 */
@Entity
@Table(name = "variante")
public class Variante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Code stable, ex. {@code INCITATION_FORTE}. */
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    /** {@code INCITER}, {@code NEUTRE} ou {@code DISSUADER}. */
    @Column(name = "intention", length = 20, nullable = false)
    private String intention;

    /** {@code ConfigBouton} sérialisé (colonne {@code JSON}). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false)
    private JsonNode config;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getIntention() {
        return intention;
    }

    public void setIntention(String intention) {
        this.intention = intention;
    }

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }
}
