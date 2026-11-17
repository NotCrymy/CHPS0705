package fr.kapture.api.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.kapture.api.dto.AffichageRequest;
import fr.kapture.api.dto.ApparitionRequest;
import fr.kapture.api.dto.EntreeChronologie;
import fr.kapture.api.dto.InteractionRequest;
import fr.kapture.api.dto.OrigineAffichage;
import fr.kapture.api.dto.OrigineApparition;
import fr.kapture.api.dto.TypeInteraction;
import fr.kapture.api.entity.Affichage;
import fr.kapture.api.entity.Apparition;
import fr.kapture.api.entity.Interaction;
import fr.kapture.api.entity.Session;
import fr.kapture.api.entity.Variante;
import fr.kapture.api.exception.RequeteInvalideException;
import fr.kapture.api.repository.AffichageRepository;
import fr.kapture.api.repository.ApparitionRepository;
import fr.kapture.api.repository.InteractionRepository;
import fr.kapture.api.repository.VarianteRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Collecte v2 envoyée par l'overlay : affichages, apparitions de bugs,
 * interactions avec le bouton ({@code docs/V2.md} §5.3).
 *
 * <p>Chaque création est publiée sur {@code /api/flux} après commit
 * ({@link FluxService#publierChronologie}).</p>
 */
@Service
@Transactional
public class CollecteService {

    private final ReferencesSession references;
    private final AffichageRepository affichages;
    private final ApparitionRepository apparitions;
    private final InteractionRepository interactions;
    private final VarianteRepository variantes;
    private final FluxService flux;
    private final JsonMapper jsonMapper;

    public CollecteService(ReferencesSession references, AffichageRepository affichages,
            ApparitionRepository apparitions, InteractionRepository interactions,
            VarianteRepository variantes, FluxService flux, JsonMapper jsonMapper) {
        this.references = references;
        this.affichages = affichages;
        this.apparitions = apparitions;
        this.interactions = interactions;
        this.variantes = variantes;
        this.flux = flux;
        this.jsonMapper = jsonMapper;
    }

    /**
     * POST /api/sessions/{id}/affichages — enregistre la variante rendue et
     * ferme l'affichage précédent encore ouvert (fin = début du nouveau).
     */
    public Long creerAffichage(Integer idSession, AffichageRequest requete) {
        Session session = references.session(idSession);
        if (!OrigineAffichage.estValide(requete.origine())) {
            throw new RequeteInvalideException("Origine d'affichage inconnue (DEFAUT ou OPERATEUR).");
        }
        Variante variante = null;
        if (requete.varianteId() != null) {
            variante = variantes.findById(requete.varianteId())
                    .orElseThrow(() -> new RequeteInvalideException("Variante inconnue."));
        }
        Instant debut = ReferencesSession.parserDate(requete.timestamp(), "Timestamp invalide.");

        fermer(idSession, debut);

        Affichage affichage = new Affichage();
        affichage.setSession(session);
        affichage.setVariante(variante);
        affichage.setConfig(jsonMapper.valueToTree(requete.config()));
        affichage.setOrigine(requete.origine());
        affichage.setDebut(debut);
        affichages.save(affichage);

        flux.publierChronologie(EntreeChronologie.affichage(affichage));
        return affichage.getId();
    }

    /** POST /api/sessions/{id}/apparitions — un bug est apparu dans le jeu. */
    public Long creerApparition(Integer idSession, ApparitionRequest requete) {
        Session session = references.session(idSession);
        if (!OrigineApparition.estValide(requete.origine())) {
            throw new RequeteInvalideException("Origine d'apparition inconnue (JEU ou COMMANDE).");
        }
        Apparition apparition = new Apparition();
        apparition.setSession(session);
        apparition.setTitre(requete.titre());
        apparition.setOrigine(requete.origine());
        apparition.setCommande(references.commande(session, requete.commandeId()));
        apparition.setAffichage(references.affichage(session, requete.affichageId()));
        apparition.setPayload(ReferencesSession.objetOptionnel(requete.payload(),
                "Le payload doit être un objet JSON."));
        apparition.setTimestamp(ReferencesSession.parserDate(requete.timestamp(), "Timestamp invalide."));
        apparitions.save(apparition);

        flux.publierChronologie(EntreeChronologie.apparition(apparition));
        return apparition.getId();
    }

    /** POST /api/sessions/{id}/interactions — survol, ouverture ou annulation. */
    public Long creerInteraction(Integer idSession, InteractionRequest requete) {
        Session session = references.session(idSession);
        if (!TypeInteraction.estValide(requete.type())) {
            throw new RequeteInvalideException("Type d'interaction inconnu (SURVOL, OUVERTURE ou ANNULATION).");
        }
        Interaction interaction = new Interaction();
        interaction.setSession(session);
        interaction.setType(requete.type());
        interaction.setAffichage(references.affichage(session, requete.affichageId()));
        interaction.setDetails(ReferencesSession.objetOptionnel(requete.details(),
                "Les détails doivent être un objet JSON."));
        interaction.setTimestamp(ReferencesSession.parserDate(requete.timestamp(), "Timestamp invalide."));
        interactions.save(interaction);

        flux.publierChronologie(EntreeChronologie.interaction(interaction));
        return interaction.getId();
    }

    /**
     * Ferme les affichages encore ouverts de la session ({@code fin} = l'instant
     * donné). Appelé à la déconnexion du dernier overlay.
     */
    public void fermerAffichageOuvert(Integer idSession, Instant fin) {
        fermer(idSession, fin);
    }

    /**
     * {@code fin} jamais antérieure au début (horloges client/serveur
     * décalées) : la durée d'affichage est alors nulle, pas négative.
     */
    private void fermer(Integer idSession, Instant fin) {
        affichages.findBySession_IdAndFinIsNull(idSession)
                .forEach(a -> a.setFin(fin.isBefore(a.getDebut()) ? a.getDebut() : fin));
    }
}
