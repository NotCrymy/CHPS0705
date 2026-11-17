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

import fr.kapture.api.dto.CommandeRequest;
import fr.kapture.api.dto.CommandeResponse;
import fr.kapture.api.dto.ResultatCommandeRequest;
import fr.kapture.api.entity.Commande;
import fr.kapture.api.entity.Session;
import fr.kapture.api.exception.ConflitException;
import fr.kapture.api.exception.RequeteInvalideException;
import fr.kapture.api.repository.CommandeRepository;
import tools.jackson.databind.json.JsonMapper;

/** Règles des commandes : ordre des contrôles, 409, cycle de vie. */
class CommandeServiceTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private CommandeRepository commandes;
    private FluxService flux;
    private CommandeService service;
    private Session session;

    @BeforeEach
    void preparer() {
        ReferencesSession references = mock(ReferencesSession.class);
        commandes = mock(CommandeRepository.class);
        flux = mock(FluxService.class);
        service = new CommandeService(references, commandes, flux);
        session = new Session();
        session.setId(42);
        when(references.session(42)).thenReturn(session);
        when(commandes.save(any(Commande.class))).thenAnswer(i -> {
            Commande c = i.getArgument(0);
            c.setId(9L);
            return c;
        });
    }

    private CommandeRequest requete(String type, String parametres) {
        return new CommandeRequest(type, parametres == null ? null : jsonMapper.readTree(parametres));
    }

    @Test
    void typeInconnu() {
        assertThatThrownBy(() -> service.creerCommande(42, requete("REDEMARRER", "{\"titre\": \"BUG3\"}")))
                .isInstanceOf(RequeteInvalideException.class)
                .hasMessage("Type de commande inconnu.");
    }

    @Test
    void titreVideOuAbsent() {
        assertThatThrownBy(() -> service.creerCommande(42, requete("DECLENCHER_BUG", "{\"titre\": \"  \"}")))
                .hasMessage("Le titre du bug est obligatoire.");
        assertThatThrownBy(() -> service.creerCommande(42, requete("DECLENCHER_BUG", null)))
                .hasMessage("Le titre du bug est obligatoire.");
    }

    @Test
    void sansOverlayConnecte409() {
        when(flux.nombreOverlays(42)).thenReturn(0);
        assertThatThrownBy(() -> service.creerCommande(42, requete("DECLENCHER_BUG", "{\"titre\": \"BUG3\"}")))
                .isInstanceOf(ConflitException.class)
                .hasMessage("Aucun overlay connecté pour cette session.");
        verify(commandes, never()).save(any());
    }

    @Test
    void avecOverlayLaCommandeEstEnvoyee() {
        when(flux.nombreOverlays(42)).thenReturn(1);
        CommandeResponse reponse = service.creerCommande(42, requete("DECLENCHER_BUG", "{\"titre\": \"BUG3\"}"));
        assertThat(reponse.id()).isEqualTo(9L);
        assertThat(reponse.statut()).isEqualTo("ENVOYEE");
        assertThat(reponse.creeA()).isNotNull();
        verify(flux).publierAuxOverlays(eq(42), eq("commande"), eq(reponse));
        verify(flux).publierChronologie(any());
    }

    @Test
    void resultatEchecPuisDejaTraitee() {
        Commande commande = new Commande();
        commande.setId(9L);
        commande.setSession(session);
        commande.setStatut("ENVOYEE");
        when(commandes.findById(9L)).thenReturn(Optional.of(commande));

        CommandeResponse reponse = service.enregistrerResultat(9L, new ResultatCommandeRequest(false, "Bug introuvable"));
        assertThat(reponse.statut()).isEqualTo("ECHEC");
        assertThat(reponse.message()).isEqualTo("Bug introuvable");
        assertThat(reponse.traiteA()).isNotNull();

        assertThatThrownBy(() -> service.enregistrerResultat(9L, new ResultatCommandeRequest(true, null)))
                .isInstanceOf(ConflitException.class)
                .hasMessage("Commande déjà traitée.");
    }
}
