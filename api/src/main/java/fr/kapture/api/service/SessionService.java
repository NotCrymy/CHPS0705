package fr.kapture.api.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import fr.kapture.api.config.InitialisationCatalogue;
import fr.kapture.api.dto.AssignationVarianteRequest;
import fr.kapture.api.dto.EntreeChronologie;
import fr.kapture.api.dto.OrigineAffichage;
import fr.kapture.api.dto.SessionEtat;
import fr.kapture.api.dto.VarianteCourante;
import fr.kapture.api.dto.VarianteResponse;
import fr.kapture.api.entity.Session;
import fr.kapture.api.entity.Variante;
import fr.kapture.api.exception.RequeteInvalideException;
import fr.kapture.api.repository.AffichageRepository;
import fr.kapture.api.repository.SessionRepository;
import fr.kapture.api.repository.VarianteRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * État en direct des sessions (v2) : catalogue, variante courante, overlays
 * connectés, assignation d'une variante par l'opérateur.
 *
 * <p>La variante courante d'une session est, dans l'ordre :</p>
 * <ol>
 *   <li>la dernière assignée par {@code PUT …/variante} (en mémoire, perdue au
 *       redémarrage de l'API) ;</li>
 *   <li>à défaut, la config du dernier affichage de la session ;</li>
 *   <li>à défaut, {@code NEUTRE} avec l'origine {@code DEFAUT}.</li>
 * </ol>
 */
@Service
@Transactional
public class SessionService {

    private final SessionRepository sessions;
    private final VarianteRepository variantes;
    private final AffichageRepository affichages;
    private final ReferencesSession references;
    private final CollecteService collecte;
    private final FluxService flux;
    private final JsonMapper jsonMapper;

    /** Assignation courante par session (thread-safe). */
    private final Map<Integer, VarianteCourante> assignations = new ConcurrentHashMap<>();

    public SessionService(SessionRepository sessions, VarianteRepository variantes,
            AffichageRepository affichages, ReferencesSession references, CollecteService collecte,
            FluxService flux, JsonMapper jsonMapper) {
        this.sessions = sessions;
        this.variantes = variantes;
        this.affichages = affichages;
        this.references = references;
        this.collecte = collecte;
        this.flux = flux;
        this.jsonMapper = jsonMapper;
    }

    /** GET /api/variantes — catalogue dans l'ordre d'insertion. */
    @Transactional(readOnly = true)
    public List<VarianteResponse> listerVariantes() {
        return variantes.findAllByOrderByIdAsc().stream().map(VarianteResponse::depuis).toList();
    }

    /** GET /api/sessions — toutes les sessions et leur état en direct. */
    @Transactional(readOnly = true)
    public List<SessionEtat> listerSessions() {
        return sessions.toutesAvecUtilisateur().stream().map(this::etat).toList();
    }

    /**
     * PUT /api/sessions/{id}/variante — assigne une variante du catalogue
     * <em>ou</em> une config personnalisée, et la pousse aux overlays connectés
     * (événement {@code variante}).
     */
    public SessionEtat assignerVariante(Integer idSession, AssignationVarianteRequest requete) {
        Session session = references.session(idSession);
        boolean parVariante = requete.varianteId() != null;
        boolean parConfig = requete.config() != null;
        if (parVariante == parConfig) {
            throw new RequeteInvalideException("Indiquer variante_id ou config.");
        }
        VarianteCourante courante;
        if (parVariante) {
            Variante variante = variantes.findById(requete.varianteId())
                    .orElseThrow(() -> new RequeteInvalideException("Variante inconnue."));
            courante = new VarianteCourante(variante.getId(), variante.getCode(),
                    OrigineAffichage.OPERATEUR.name(), variante.getConfig());
        } else {
            courante = new VarianteCourante(null, null, OrigineAffichage.OPERATEUR.name(),
                    jsonMapper.valueToTree(requete.config()));
        }
        assignations.put(idSession, courante);
        flux.publierAuxOverlays(idSession, "variante", courante);
        return etat(session);
    }

    /**
     * GET /api/sessions/{id}/flux — ouvre le flux SSE d'un overlay : envoie la
     * variante courante, publie {@code CONNEXION} à la console. À la
     * disparition du dernier overlay : l'affichage ouvert est fermé
     * (fin = maintenant) puis {@code CONNEXION} est publiée.
     */
    public SseEmitter ouvrirFluxOverlay(Integer idSession) {
        Session session = references.session(idSession);
        VarianteCourante courante = varianteCourante(session.getId());
        SseEmitter emetteur = flux.abonnerOverlay(idSession, courante, () -> surDeconnexion(idSession));
        flux.publierChronologie(EntreeChronologie.connexion(idSession, flux.nombreOverlays(idSession), maintenant()));
        return emetteur;
    }

    /**
     * Appelé hors requête (thread du {@link FluxService}) : aucune transaction
     * en cours, {@link CollecteService#fermerAffichageOuvert} ouvre la sienne
     * et la publication part tout de suite.
     */
    private void surDeconnexion(Integer idSession) {
        int restants = flux.nombreOverlays(idSession);
        if (restants == 0) {
            collecte.fermerAffichageOuvert(idSession, maintenant());
        }
        flux.publierChronologie(EntreeChronologie.connexion(idSession, restants, maintenant()));
    }

    private SessionEtat etat(Session session) {
        int nombre = flux.nombreOverlays(session.getId());
        return new SessionEtat(session.getId(), session.getUtilisateur().getPseudonyme(),
                session.getApplication(), session.getDebut(), nombre > 0, nombre,
                varianteCourante(session.getId()));
    }

    /** Assignation en mémoire, sinon dernier affichage, sinon NEUTRE / DEFAUT. */
    VarianteCourante varianteCourante(Integer idSession) {
        VarianteCourante assignee = assignations.get(idSession);
        if (assignee != null) {
            return assignee;
        }
        return affichages.findFirstBySession_IdOrderByDebutDescIdDesc(idSession)
                .map(a -> new VarianteCourante(
                        a.getVariante() == null ? null : a.getVariante().getId(),
                        a.getVariante() == null ? null : a.getVariante().getCode(),
                        a.getOrigine(), a.getConfig()))
                .orElseGet(this::neutre);
    }

    private VarianteCourante neutre() {
        return variantes.findByCode(InitialisationCatalogue.CODE_NEUTRE)
                .map(v -> new VarianteCourante(v.getId(), v.getCode(), OrigineAffichage.DEFAUT.name(), v.getConfig()))
                .orElseGet(() -> new VarianteCourante(null, InitialisationCatalogue.CODE_NEUTRE,
                        OrigineAffichage.DEFAUT.name(), jsonMapper.valueToTree(InitialisationCatalogue.CONFIG_NEUTRE)));
    }

    private static Instant maintenant() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
