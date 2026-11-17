package fr.kapture.api.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.kapture.api.dto.ConfigBouton;
import fr.kapture.api.dto.ConfigBouton.Badge;
import fr.kapture.api.dto.Intention;
import fr.kapture.api.entity.Variante;
import fr.kapture.api.repository.VarianteRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Insère le catalogue initial de variantes ({@code docs/V2.md} §5.2) au
 * démarrage, seulement si la table {@code variante} est vide : les variantes
 * modifiées ou ajoutées à la main ne sont jamais écrasées.
 */
@Component
public class InitialisationCatalogue implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InitialisationCatalogue.class);

    /** Code de la variante par défaut d'une session sans assignation. */
    public static final String CODE_NEUTRE = "NEUTRE";

    /** Config de {@code NEUTRE}, aussi utilisée en secours si le catalogue est vide. */
    public static final ConfigBouton CONFIG_NEUTRE = new ConfigBouton(true, "#64748b", "M", 1.0,
            "Signaler", new Badge("aucun", null), "aucune", "bas-droite");

    /** Une ligne du catalogue initial. */
    record Definition(String code, String nom, Intention intention, ConfigBouton config) {
    }

    static final List<Definition> CATALOGUE = List.of(
            new Definition(CODE_NEUTRE, "Neutre", Intention.NEUTRE, CONFIG_NEUTRE),
            new Definition("INCITATION_DOUCE", "Incitation douce", Intention.INCITER,
                    new ConfigBouton(true, "#7c3aed", "M", 1.0, "Signaler un bug",
                            new Badge("aucun", null), "respiration", "bas-droite")),
            new Definition("INCITATION_FORTE", "Incitation forte", Intention.INCITER,
                    new ConfigBouton(true, "#f43f5e", "L", 1.0, "Un bug ? Signale-le !",
                            new Badge("texte", "!"), "pulsation", "bas-droite")),
            new Definition("INCITATION_COMPTEUR", "Compteur d'apparitions", Intention.INCITER,
                    new ConfigBouton(true, "#f59e0b", "L", 1.0, "Signaler",
                            new Badge("compteur", null), "rebond", "bas-droite")),
            new Definition("DISSUASION_DISCRETE", "Discrète", Intention.DISSUADER,
                    new ConfigBouton(true, "#475569", "S", 0.35, "",
                            new Badge("aucun", null), "aucune", "haut-gauche")),
            new Definition("MASQUE", "Masqué", Intention.DISSUADER,
                    new ConfigBouton(false, "#64748b", "M", 1.0, "Signaler",
                            new Badge("aucun", null), "aucune", "bas-droite")));

    private final VarianteRepository variantes;
    private final JsonMapper jsonMapper;

    public InitialisationCatalogue(VarianteRepository variantes, JsonMapper jsonMapper) {
        this.variantes = variantes;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (variantes.count() > 0) {
            return;
        }
        for (Definition definition : CATALOGUE) {
            Variante variante = new Variante();
            variante.setCode(definition.code());
            variante.setNom(definition.nom());
            variante.setIntention(definition.intention().name());
            variante.setConfig(jsonMapper.valueToTree(definition.config()));
            variantes.save(variante);
        }
        LOG.info("Catalogue de variantes initialisé ({} variantes)", CATALOGUE.size());
    }
}
