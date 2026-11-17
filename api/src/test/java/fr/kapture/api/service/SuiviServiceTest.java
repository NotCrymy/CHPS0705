package fr.kapture.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.kapture.api.dto.StatVariante;
import fr.kapture.api.entity.Affichage;
import fr.kapture.api.entity.Variante;
import fr.kapture.api.repository.AffichageRepository;
import fr.kapture.api.repository.ApparitionRepository;
import fr.kapture.api.repository.CommandeRepository;
import fr.kapture.api.repository.EvenementRepository;
import fr.kapture.api.repository.InteractionRepository;
import fr.kapture.api.repository.VarianteRepository;

/** Agrégats de {@code GET /api/suivi/variantes}. */
class SuiviServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-17T14:00:00Z");

    private VarianteRepository variantes;
    private AffichageRepository affichages;
    private ApparitionRepository apparitions;
    private InteractionRepository interactions;
    private EvenementRepository evenements;
    private SuiviService service;

    @BeforeEach
    void preparer() {
        variantes = mock(VarianteRepository.class);
        affichages = mock(AffichageRepository.class);
        apparitions = mock(ApparitionRepository.class);
        interactions = mock(InteractionRepository.class);
        evenements = mock(EvenementRepository.class);
        service = new SuiviService(mock(ReferencesSession.class), variantes, affichages,
                mock(CommandeRepository.class), apparitions, interactions, evenements);
    }

    private static Variante variante(int id, String code, String intention) {
        Variante v = new Variante();
        v.setId(id);
        v.setCode(code);
        v.setNom(code.toLowerCase());
        v.setIntention(intention);
        return v;
    }

    private static Affichage affichage(long id, Variante variante, Instant debut, Instant fin) {
        Affichage a = new Affichage();
        a.setId(id);
        a.setVariante(variante);
        a.setDebut(debut);
        a.setFin(fin);
        return a;
    }

    @Test
    void agregeParVarianteAvecPersonnaliseeEtAffichageOuvert() {
        Variante neutre = variante(1, "NEUTRE", "NEUTRE");
        Variante forte = variante(3, "INCITATION_FORTE", "INCITER");
        Variante masque = variante(6, "MASQUE", "DISSUADER");
        when(variantes.findAllByOrderByIdAsc()).thenReturn(List.of(neutre, forte, masque));
        when(affichages.pourSuivi(any())).thenReturn(List.of(
                affichage(10, neutre, T0, T0.plusSeconds(60)),
                affichage(11, forte, T0.plusSeconds(60), T0.plusSeconds(160)),
                affichage(12, forte, T0.plusSeconds(200), null),          // ouvert : jusqu'à « maintenant »
                affichage(13, null, T0.plusSeconds(160), T0.plusSeconds(200))));
        // 3 apparitions sous INCITATION_FORTE, 1 sous NEUTRE
        when(apparitions.affichagesDesApparitions(any())).thenReturn(List.of(11L, 11L, 12L, 10L));
        when(interactions.affichagesEtTypes(any())).thenReturn(List.of(
                new Object[] {11L, "SURVOL"}, new Object[] {11L, "OUVERTURE"},
                new Object[] {12L, "ANNULATION"}, new Object[] {13L, "SURVOL"}));
        // 3 signalements sous INCITATION_FORTE dont 2 couvrent la même apparition 100
        when(evenements.affichagesEtApparitions(any())).thenReturn(List.of(
                new Object[] {11L, 100L}, new Object[] {12L, 100L}, new Object[] {12L, null},
                new Object[] {13L, 101L}));

        List<StatVariante> stats = service.statistiquesVariantes(null, T0.plusSeconds(250));

        assertThat(stats).extracting(StatVariante::code)
                .containsExactly("NEUTRE", "INCITATION_FORTE", "MASQUE", "PERSONNALISEE");

        StatVariante neutreStat = stats.get(0);
        assertThat(neutreStat.affichages()).isEqualTo(1);
        assertThat(neutreStat.dureeMs()).isEqualTo(60_000);
        assertThat(neutreStat.apparitions()).isEqualTo(1);
        assertThat(neutreStat.tauxSignalement()).isEqualTo(0.0);

        StatVariante forteStat = stats.get(1);
        assertThat(forteStat.varianteId()).isEqualTo(3);
        assertThat(forteStat.intention()).isEqualTo("INCITER");
        assertThat(forteStat.affichages()).isEqualTo(2);
        assertThat(forteStat.dureeMs()).isEqualTo(100_000 + 50_000);
        assertThat(forteStat.apparitions()).isEqualTo(3);
        assertThat(forteStat.signalements()).isEqualTo(3);
        assertThat(forteStat.signalementsCouvrantApparition()).isEqualTo(1);
        assertThat(forteStat.survols()).isEqualTo(1);
        assertThat(forteStat.ouvertures()).isEqualTo(1);
        assertThat(forteStat.annulations()).isEqualTo(1);
        assertThat(forteStat.tauxSignalement()).isEqualTo(1.0 / 3);

        StatVariante masqueStat = stats.get(2);
        assertThat(masqueStat.affichages()).isZero();
        assertThat(masqueStat.tauxSignalement()).isNull();

        StatVariante perso = stats.get(3);
        assertThat(perso.varianteId()).isNull();
        assertThat(perso.affichages()).isEqualTo(1);
        assertThat(perso.dureeMs()).isEqualTo(40_000);
        assertThat(perso.survols()).isEqualTo(1);
        assertThat(perso.signalements()).isEqualTo(1);
        assertThat(perso.tauxSignalement()).isNull();
    }

    @Test
    void pasDeLignePersonnaliseeSansConfigAdHoc() {
        when(variantes.findAllByOrderByIdAsc()).thenReturn(List.of(variante(1, "NEUTRE", "NEUTRE")));
        when(affichages.pourSuivi(42)).thenReturn(List.of());
        assertThat(service.statistiquesVariantes(42, T0)).extracting(StatVariante::code).containsExactly("NEUTRE");
    }
}
