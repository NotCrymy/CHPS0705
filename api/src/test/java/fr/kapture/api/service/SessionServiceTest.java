package fr.kapture.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import fr.kapture.api.config.InitialisationCatalogue;
import fr.kapture.api.dto.AssignationVarianteRequest;
import fr.kapture.api.dto.SessionEtat;
import fr.kapture.api.dto.VarianteCourante;
import fr.kapture.api.entity.Appariement;
import fr.kapture.api.entity.Session;
import fr.kapture.api.entity.Variante;
import fr.kapture.api.exception.RequeteInvalideException;
import fr.kapture.api.repository.AffichageRepository;
import fr.kapture.api.repository.SessionRepository;
import fr.kapture.api.repository.VarianteRepository;
import tools.jackson.databind.json.JsonMapper;

/** Règles de {@code PUT …/variante} et résolution de la variante courante. */
class SessionServiceTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private VarianteRepository variantes;
    private AffichageRepository affichages;
    private ReferencesSession references;
    private FluxService flux;
    private SessionService service;
    private Session session;

    @BeforeEach
    void preparer() {
        variantes = mock(VarianteRepository.class);
        affichages = mock(AffichageRepository.class);
        references = mock(ReferencesSession.class);
        flux = mock(FluxService.class);
        service = new SessionService(mock(SessionRepository.class), variantes, affichages, references,
                mock(CollecteService.class), flux, jsonMapper);

        Appariement utilisateur = new Appariement();
        utilisateur.setPseudonyme("Storm_X9");
        session = new Session();
        session.setId(42);
        session.setUtilisateur(utilisateur);
        session.setApplication("arena_fps");
        when(references.session(42)).thenReturn(session);
        when(affichages.findFirstBySession_IdOrderByDebutDescIdDesc(42)).thenReturn(Optional.empty());
    }

    private Variante variante(int id, String code) {
        Variante variante = new Variante();
        variante.setId(id);
        variante.setCode(code);
        variante.setConfig(jsonMapper.valueToTree(InitialisationCatalogue.CONFIG_NEUTRE));
        return variante;
    }

    @Test
    void niVarianteNiConfigEstRefuse() {
        assertThatThrownBy(() -> service.assignerVariante(42, new AssignationVarianteRequest(null, null)))
                .isInstanceOf(RequeteInvalideException.class)
                .hasMessage("Indiquer variante_id ou config.");
    }

    @Test
    void varianteEtConfigEnsembleSontRefuses() {
        assertThatThrownBy(() -> service.assignerVariante(42,
                new AssignationVarianteRequest(3, InitialisationCatalogue.CONFIG_NEUTRE)))
                .hasMessage("Indiquer variante_id ou config.");
    }

    @Test
    void varianteInconnueEstRefusee() {
        when(variantes.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assignerVariante(42, new AssignationVarianteRequest(99, null)))
                .hasMessage("Variante inconnue.");
        verify(flux, never()).publierAuxOverlays(any(), any(), any());
    }

    @Test
    void assignationPousseeAuxOverlaysEtRetenue() {
        when(variantes.findById(3)).thenReturn(Optional.of(variante(3, "INCITATION_FORTE")));
        when(flux.nombreOverlays(42)).thenReturn(2);

        SessionEtat etat = service.assignerVariante(42, new AssignationVarianteRequest(3, null));

        ArgumentCaptor<Object> poussee = ArgumentCaptor.forClass(Object.class);
        verify(flux).publierAuxOverlays(eq(42), eq("variante"), poussee.capture());
        assertThat(poussee.getValue()).isEqualTo(etat.variante());
        assertThat(etat.variante().varianteId()).isEqualTo(3);
        assertThat(etat.variante().origine()).isEqualTo("OPERATEUR");
        assertThat(etat.connecte()).isTrue();
        assertThat(etat.overlays()).isEqualTo(2);
        assertThat(service.varianteCourante(42)).isEqualTo(etat.variante());
    }

    @Test
    void configPersonnaliseeSansIdNiCode() {
        SessionEtat etat = service.assignerVariante(42,
                new AssignationVarianteRequest(null, InitialisationCatalogue.CONFIG_NEUTRE));
        assertThat(etat.variante().varianteId()).isNull();
        assertThat(etat.variante().code()).isNull();
        assertThat(etat.variante().config().get("couleur").asString()).isEqualTo("#64748b");
    }

    @Test
    void sansAssignationNiAffichageCestNeutreParDefaut() {
        when(variantes.findByCode("NEUTRE")).thenReturn(Optional.of(variante(1, "NEUTRE")));
        VarianteCourante courante = service.varianteCourante(42);
        assertThat(courante.code()).isEqualTo("NEUTRE");
        assertThat(courante.origine()).isEqualTo("DEFAUT");
        verify(affichages).findFirstBySession_IdOrderByDebutDescIdDesc(42);
    }
}
