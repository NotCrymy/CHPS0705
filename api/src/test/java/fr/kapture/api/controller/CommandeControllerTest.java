package fr.kapture.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kapture.api.dto.CommandeRequest;
import fr.kapture.api.dto.CommandeResponse;
import fr.kapture.api.dto.ResultatCommandeRequest;
import fr.kapture.api.exception.ConflitException;
import fr.kapture.api.exception.RessourceIntrouvableException;
import fr.kapture.api.service.CommandeService;
import tools.jackson.databind.json.JsonMapper;

/**
 * Routes des commandes : 201, 409 sans overlay, 409 déjà traitée, 404.
 * Service mocké.
 */
@WebMvcTest(CommandeController.class)
class CommandeControllerTest {

    private static final String CORPS = "{\"type\": \"DECLENCHER_BUG\", \"parametres\": {\"titre\": \"BUG3\"}}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandeService commandeService;

    @Test
    void creationRenvoie201AvecLaCommande() throws Exception {
        CommandeResponse commande = new CommandeResponse(9L, 42, "DECLENCHER_BUG",
                JsonMapper.builder().build().readTree("{\"titre\": \"BUG3\"}"), "ENVOYEE", null,
                Instant.parse("2026-11-17T14:32:05.120Z"), null);
        when(commandeService.creerCommande(eq(42), any(CommandeRequest.class))).thenReturn(commande);

        mockMvc.perform(post("/api/sessions/42/commandes")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.session_id").value(42))
                .andExpect(jsonPath("$.parametres.titre").value("BUG3"))
                .andExpect(jsonPath("$.statut").value("ENVOYEE"))
                .andExpect(jsonPath("$.cree_a").value("2026-11-17T14:32:05.120Z"))
                .andExpect(jsonPath("$.traite_a").doesNotExist());
    }

    @Test
    void creationSansOverlayRenvoie409() throws Exception {
        when(commandeService.creerCommande(eq(42), any(CommandeRequest.class)))
                .thenThrow(new ConflitException("Aucun overlay connecté pour cette session."));

        mockMvc.perform(post("/api/sessions/42/commandes")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Aucun overlay connecté pour cette session."));
    }

    @Test
    void resultatDejaTraiteRenvoie409() throws Exception {
        when(commandeService.enregistrerResultat(eq(9L), any(ResultatCommandeRequest.class)))
                .thenThrow(new ConflitException("Commande déjà traitée."));

        mockMvc.perform(post("/api/commandes/9/resultat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"succes\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Commande déjà traitée."));
    }

    @Test
    void resultatCommandeInconnueRenvoie404() throws Exception {
        when(commandeService.enregistrerResultat(eq(999L), any(ResultatCommandeRequest.class)))
                .thenThrow(new RessourceIntrouvableException("Commande inconnue."));

        mockMvc.perform(post("/api/commandes/999/resultat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"succes\": false, \"message\": \"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Commande inconnue."));
    }

    @Test
    void resultatSansSuccesRenvoie400() throws Exception {
        mockMvc.perform(post("/api/commandes/9/resultat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le résultat (succes) est obligatoire."));
    }
}
