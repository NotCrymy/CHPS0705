package fr.kapture.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kapture.api.dto.AffichageRequest;
import fr.kapture.api.dto.ApparitionRequest;
import fr.kapture.api.dto.InteractionRequest;
import fr.kapture.api.exception.RequeteInvalideException;
import fr.kapture.api.service.CollecteService;

/**
 * Routes de collecte de l'overlay : 201 {@code {id}}, validation Jakarta
 * (config imbriquée comprise), erreurs métier. Service mocké.
 */
@WebMvcTest(CollecteController.class)
class CollecteControllerTest {

    private static final String CONFIG = """
            {"visible": true, "couleur": "#64748b", "taille": "M", "opacite": 1,
             "libelle": "Signaler", "badge": {"mode": "aucun"},
             "animation": "aucune", "position": "bas-droite"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollecteService collecteService;

    @Test
    void affichageValideRenvoie201() throws Exception {
        when(collecteService.creerAffichage(eq(42), any(AffichageRequest.class))).thenReturn(51L);

        mockMvc.perform(post("/api/sessions/42/affichages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variante_id\": 1, \"config\": " + CONFIG
                                + ", \"origine\": \"DEFAUT\", \"timestamp\": \"2026-11-17T14:32:05.120Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(51));
    }

    @Test
    void affichageSansConfigRenvoie400() throws Exception {
        mockMvc.perform(post("/api/sessions/42/affichages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origine\": \"DEFAUT\", \"timestamp\": \"2026-11-17T14:32:05.120Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La configuration est obligatoire."));
    }

    @Test
    void affichageAvecConfigInvalideRenvoie400() throws Exception {
        String config = CONFIG.replace("\"aucune\"", "\"toupie\"");
        mockMvc.perform(post("/api/sessions/42/affichages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"config\": " + config
                                + ", \"origine\": \"DEFAUT\", \"timestamp\": \"2026-11-17T14:32:05.120Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Animation invalide."));
    }

    @Test
    void apparitionAvecAffichageEtrangerRenvoie400() throws Exception {
        when(collecteService.creerApparition(eq(42), any(ApparitionRequest.class)))
                .thenThrow(new RequeteInvalideException("Affichage inconnu."));

        mockMvc.perform(post("/api/sessions/42/apparitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\": \"BUG3\", \"origine\": \"JEU\", \"affichage_id\": 999,"
                                + " \"timestamp\": \"2026-11-17T14:32:05.120Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Affichage inconnu."));
    }

    @Test
    void apparitionSansTitreRenvoie400() throws Exception {
        mockMvc.perform(post("/api/sessions/42/apparitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origine\": \"JEU\", \"timestamp\": \"2026-11-17T14:32:05.120Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le titre est obligatoire."));
    }

    @Test
    void interactionValideRenvoie201() throws Exception {
        when(collecteService.creerInteraction(eq(42), any(InteractionRequest.class))).thenReturn(7L);

        mockMvc.perform(post("/api/sessions/42/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"SURVOL\", \"affichage_id\": 51, \"details\": {\"duree_ms\": 800},"
                                + " \"timestamp\": \"2026-11-17T14:32:05.120Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }
}
