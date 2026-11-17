package fr.kapture.api.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;

import fr.kapture.api.entity.Affichage;
import fr.kapture.api.entity.Apparition;
import fr.kapture.api.entity.Commande;
import fr.kapture.api.entity.Session;
import fr.kapture.api.exception.RequeteInvalideException;
import fr.kapture.api.exception.RessourceIntrouvableException;
import fr.kapture.api.repository.AffichageRepository;
import fr.kapture.api.repository.ApparitionRepository;
import fr.kapture.api.repository.CommandeRepository;
import fr.kapture.api.repository.SessionRepository;
import tools.jackson.databind.JsonNode;

/**
 * Contrôles partagés par les services v2 (et {@link BugService}) : session
 * existante (404), identifiants optionnels {@code affichage_id},
 * {@code apparition_id}, {@code commande_id} existants <em>et</em> rattachés à
 * la même session (400), dates ISO 8601, objets JSON.
 *
 * <p>S'exécute dans la transaction de l'appelant.</p>
 */
@Component
public class ReferencesSession {

    private final SessionRepository sessions;
    private final AffichageRepository affichages;
    private final ApparitionRepository apparitions;
    private final CommandeRepository commandes;

    public ReferencesSession(SessionRepository sessions, AffichageRepository affichages,
            ApparitionRepository apparitions, CommandeRepository commandes) {
        this.sessions = sessions;
        this.affichages = affichages;
        this.apparitions = apparitions;
        this.commandes = commandes;
    }

    /** Session existante, sinon 404 « Session inconnue. ». */
    public Session session(Integer idSession) {
        return sessions.findById(idSession)
                .orElseThrow(() -> new RessourceIntrouvableException("Session inconnue."));
    }

    /** {@code null} si {@code id} est nul ; sinon affichage de la session, ou 400 « Affichage inconnu. ». */
    public Affichage affichage(Session session, Long id) {
        if (id == null) {
            return null;
        }
        return affichages.findById(id)
                .filter(a -> a.getSession().getId().equals(session.getId()))
                .orElseThrow(() -> new RequeteInvalideException("Affichage inconnu."));
    }

    /** {@code null} si {@code id} est nul ; sinon apparition de la session, ou 400 « Apparition inconnue. ». */
    public Apparition apparition(Session session, Long id) {
        if (id == null) {
            return null;
        }
        return apparitions.findById(id)
                .filter(a -> a.getSession().getId().equals(session.getId()))
                .orElseThrow(() -> new RequeteInvalideException("Apparition inconnue."));
    }

    /** {@code null} si {@code id} est nul ; sinon commande de la session, ou 400 « Commande inconnue. ». */
    public Commande commande(Session session, Long id) {
        if (id == null) {
            return null;
        }
        return commandes.findById(id)
                .filter(c -> c.getSession().getId().equals(session.getId()))
                .orElseThrow(() -> new RequeteInvalideException("Commande inconnue."));
    }

    /** Date ISO 8601, sinon 400 avec le message fourni. */
    public static Instant parserDate(String valeur, String messageErreur) {
        try {
            return Instant.parse(valeur);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RequeteInvalideException(messageErreur);
        }
    }

    /** Un JSON optionnel doit être un objet s'il est présent ({@code null} JSON = absent). */
    public static JsonNode objetOptionnel(JsonNode valeur, String messageErreur) {
        if (valeur == null || valeur.isNull()) {
            return null;
        }
        if (!valeur.isObject()) {
            throw new RequeteInvalideException(messageErreur);
        }
        return valeur;
    }
}
