package fr.kapture.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.kapture.api.dto.EntreeChronologie;
import fr.kapture.api.dto.StatVariante;
import fr.kapture.api.dto.TypeInteraction;
import fr.kapture.api.entity.Affichage;
import fr.kapture.api.entity.Variante;
import fr.kapture.api.repository.AffichageRepository;
import fr.kapture.api.repository.ApparitionRepository;
import fr.kapture.api.repository.CommandeRepository;
import fr.kapture.api.repository.EvenementRepository;
import fr.kapture.api.repository.InteractionRepository;
import fr.kapture.api.repository.VarianteRepository;

/**
 * Lecture du suivi v2 : chronologie d'une session et statistiques par
 * variante. Agrégats calculés en Java à partir de requêtes simples (volumes
 * du POC modestes).
 */
@Service
@Transactional(readOnly = true)
public class SuiviService {

    /** Code de regroupement des configurations personnalisées. */
    static final String CODE_PERSONNALISEE = "PERSONNALISEE";

    private final ReferencesSession references;
    private final VarianteRepository variantes;
    private final AffichageRepository affichages;
    private final CommandeRepository commandes;
    private final ApparitionRepository apparitions;
    private final InteractionRepository interactions;
    private final EvenementRepository evenements;

    public SuiviService(ReferencesSession references, VarianteRepository variantes,
            AffichageRepository affichages, CommandeRepository commandes,
            ApparitionRepository apparitions, InteractionRepository interactions,
            EvenementRepository evenements) {
        this.references = references;
        this.variantes = variantes;
        this.affichages = affichages;
        this.commandes = commandes;
        this.apparitions = apparitions;
        this.interactions = interactions;
        this.evenements = evenements;
    }

    /**
     * GET /api/sessions/{id}/chronologie — affichages, commandes, apparitions,
     * interactions et signalements, triés par date croissante (puis type, id).
     * Les connexions ne sont pas persistées : elles n'apparaissent que sur le
     * flux {@code /api/flux}.
     */
    public List<EntreeChronologie> chronologie(Integer idSession) {
        references.session(idSession);
        List<EntreeChronologie> entrees = new ArrayList<>();
        affichages.chronologie(idSession).forEach(a -> entrees.add(EntreeChronologie.affichage(a)));
        commandes.findBySession_Id(idSession).forEach(c -> entrees.add(EntreeChronologie.commande(c)));
        apparitions.findBySession_Id(idSession).forEach(a -> entrees.add(EntreeChronologie.apparition(a)));
        interactions.findBySession_Id(idSession).forEach(i -> entrees.add(EntreeChronologie.interaction(i)));
        evenements.chronologie(idSession).forEach(e -> entrees.add(EntreeChronologie.signalement(e)));
        entrees.sort(Comparator.comparing(EntreeChronologie::timestamp)
                .thenComparing(EntreeChronologie::type)
                .thenComparing(EntreeChronologie::id));
        return entrees;
    }

    /**
     * GET /api/suivi/variantes — une ligne par variante du catalogue (même
     * sans affichage), plus {@code PERSONNALISEE} s'il y a eu des configs ad
     * hoc. Tout est filtré par session si {@code sessionId} est fourni.
     */
    public List<StatVariante> statistiquesVariantes(Integer sessionId) {
        return statistiquesVariantes(sessionId, Instant.now());
    }

    /** Variante testable : {@code maintenant} borne la durée des affichages encore ouverts. */
    List<StatVariante> statistiquesVariantes(Integer sessionId, Instant maintenant) {
        // Compteurs par code de variante, dans l'ordre du catalogue
        Map<String, Compteurs> parCode = new LinkedHashMap<>();
        Map<String, Variante> varianteParCode = new HashMap<>();
        for (Variante variante : variantes.findAllByOrderByIdAsc()) {
            parCode.put(variante.getCode(), new Compteurs());
            varianteParCode.put(variante.getCode(), variante);
        }

        // Affichages : nombre, durée, et table affichage_id → code
        Map<Long, String> codeParAffichage = new HashMap<>();
        for (Affichage affichage : affichages.pourSuivi(sessionId)) {
            String code = affichage.getVariante() == null ? CODE_PERSONNALISEE : affichage.getVariante().getCode();
            codeParAffichage.put(affichage.getId(), code);
            Compteurs compteurs = parCode.computeIfAbsent(code, c -> new Compteurs());
            compteurs.affichages++;
            Instant fin = affichage.getFin() == null ? maintenant : affichage.getFin();
            compteurs.dureeMs += Math.max(0, Duration.between(affichage.getDebut(), fin).toMillis());
        }

        for (Long idAffichage : apparitions.affichagesDesApparitions(sessionId)) {
            compteurs(parCode, codeParAffichage, idAffichage).apparitions++;
        }
        for (Object[] ligne : interactions.affichagesEtTypes(sessionId)) {
            Compteurs compteurs = compteurs(parCode, codeParAffichage, (Long) ligne[0]);
            String type = (String) ligne[1];
            if (TypeInteraction.SURVOL.name().equals(type)) {
                compteurs.survols++;
            } else if (TypeInteraction.OUVERTURE.name().equals(type)) {
                compteurs.ouvertures++;
            } else if (TypeInteraction.ANNULATION.name().equals(type)) {
                compteurs.annulations++;
            }
        }
        for (Object[] ligne : evenements.affichagesEtApparitions(sessionId)) {
            Compteurs compteurs = compteurs(parCode, codeParAffichage, (Long) ligne[0]);
            compteurs.signalements++;
            if (ligne[1] != null) {
                compteurs.apparitionsCouvertes.add((Long) ligne[1]);
            }
        }

        List<StatVariante> resultat = new ArrayList<>();
        parCode.forEach((code, c) -> {
            Variante variante = varianteParCode.get(code);
            if (variante == null && c.affichages == 0) {
                return; // pas de ligne PERSONNALISEE vide
            }
            long couvertes = c.apparitionsCouvertes.size();
            resultat.add(new StatVariante(
                    variante == null ? null : variante.getId(),
                    code,
                    variante == null ? "Configuration personnalisée" : variante.getNom(),
                    variante == null ? null : variante.getIntention(),
                    c.affichages, c.dureeMs, c.apparitions, c.signalements, couvertes,
                    c.survols, c.ouvertures, c.annulations,
                    c.apparitions == 0 ? null : (double) couvertes / c.apparitions));
        });
        return resultat;
    }

    /**
     * Compteurs de la variante de l'affichage. L'affichage est forcément dans
     * la table (mêmes filtres de session : un comportement est rattaché à un
     * affichage de sa propre session).
     */
    private static Compteurs compteurs(Map<String, Compteurs> parCode, Map<Long, String> codeParAffichage,
            Long idAffichage) {
        String code = codeParAffichage.getOrDefault(idAffichage, CODE_PERSONNALISEE);
        return parCode.computeIfAbsent(code, c -> new Compteurs());
    }

    /** Accumulateur mutable, local au calcul. */
    private static final class Compteurs {
        long affichages;
        long dureeMs;
        long apparitions;
        long signalements;
        /** Apparitions distinctes couvertes par au moins un signalement. */
        final Set<Long> apparitionsCouvertes = new HashSet<>();
        long survols;
        long ouvertures;
        long annulations;
    }
}
