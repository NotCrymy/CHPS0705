package fr.kapture.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kapture.api.dto.AssignationVarianteRequest;
import fr.kapture.api.dto.SessionEtat;
import fr.kapture.api.dto.VarianteCourante;
import fr.kapture.api.exception.RessourceIntrouvableException;
import fr.kapture.api.service.SessionService;
import tools.jackson.databind.json.JsonMapper;

/**
 * Routes de pilotage : validation Jakarta de {@code ConfigBouton} (messages
 * exacts du contrat v2) et forme JSON de {@code SessionEtat}. Service mocké.
 */
@WebMvcTest(SessionController.class)
class SessionControllerTest {

    private static final String CONFIG_VALIDE = """
            {"visible": true, "couleur": "#7c3aed", "taille": "M", "opacite": 1,
             "libelle": "Signaler un bug", "badge": {"mode": "texte", "texte": "!"},
             "animation": "pulsation", "position": "bas-droite"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    private static SessionEtat etat() {
        JsonMapper mapper = JsonMapper.builder().build();
        return new SessionEtat(42, "Storm_X9", "arena_fps", Instant.parse("2026-10-19T14:10:00Z"),
                true, 1, new VarianteCourante(3, "INCITATION_FORTE", "OPERATEUR",
                        mapper.readTree(CONFIG_VALIDE)));
    }

    @Test
    void putVarianteParIdRenvoieSessionEtat() throws Exception {
        when(sessionService.assignerVariante(eq(42), any(AssignationVarianteRequest.class))).thenReturn(etat());

        mockMvc.perform(put("/api/sessions/42/variante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variante_id\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.id_utilisateur").value("Storm_X9"))
                .andExpect(jsonPath("$.debut").value("2026-10-19T14:10:00.000Z"))
                .andExpect(jsonPath("$.connecte").value(true))
                .andExpect(jsonPath("$.overlays").value(1))
                .andExpect(jsonPath("$.variante.variante_id").value(3))
                .andExpect(jsonPath("$.variante.origine").value("OPERATEUR"))
                .andExpect(jsonPath("$.variante.config.couleur").value("#7c3aed"));
    }

    @Test
    void putVarianteAvecConfigValideAppelleLeService() throws Exception {
        when(sessionService.assignerVariante(eq(42), any(AssignationVarianteRequest.class))).thenReturn(etat());

        mockMvc.perform(put("/api/sessions/42/variante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"config\": " + CONFIG_VALIDE + "}"))
                .andExpect(status().isOk());
        verify(sessionService).assignerVariante(eq(42), any(AssignationVarianteRequest.class));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "\"visible\": null                        | La visibilité est obligatoire.",
            "\"couleur\": \"rouge\"                   | Couleur invalide (format #rrggbb).",
            "\"couleur\": \"#12345\"                  | Couleur invalide (format #rrggbb).",
            "\"taille\": \"XXL\"                      | Taille invalide.",
            "\"opacite\": 0.05                        | Opacité invalide (entre 0.1 et 1).",
            "\"opacite\": 1.5                         | Opacité invalide (entre 0.1 et 1).",
            "\"libelle\": \"12345678901234567890123456789012345678901\" | Le libellé ne doit pas dépasser 40 caractères.",
            "\"badge\": {\"mode\": \"clignotant\"}    | Badge invalide.",
            "\"badge\": {\"mode\": \"texte\", \"texte\": \"ABCD\"} | Le texte du badge ne doit pas dépasser 3 caractères.",
            "\"animation\": \"toupie\"                | Animation invalide.",
            "\"position\": \"centre\"                 | Position invalide."
    })
    void configInvalideRenvoie400AvecLeMessageDuContrat(String champ, String message) throws Exception {
        // le dernier champ du JSON l'emporte : on écrase la valeur valide
        String config = CONFIG_VALIDE.substring(0, CONFIG_VALIDE.length() - 1) + ", " + champ + "}";

        mockMvc.perform(put("/api/sessions/42/variante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"config\": " + config + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
        verify(sessionService, never()).assignerVariante(any(), any());
    }

    @Test
    void putVarianteSessionInconnueRenvoie404() throws Exception {
        when(sessionService.assignerVariante(eq(999), any(AssignationVarianteRequest.class)))
                .thenThrow(new RessourceIntrouvableException("Session inconnue."));

        mockMvc.perform(put("/api/sessions/999/variante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variante_id\": 3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Session inconnue."));
    }

    @Test
    void getSessionsRenvoieLaListe() throws Exception {
        when(sessionService.listerSessions()).thenReturn(List.of(etat()));

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].application").value("arena_fps"))
                .andExpect(jsonPath("$[0].variante.code").value("INCITATION_FORTE"));
    }
}
