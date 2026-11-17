package fr.kapture.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kapture.api.dto.EntreeChronologie;
import fr.kapture.api.dto.StatVariante;
import fr.kapture.api.service.SuiviService;

/**
 * Routes v2 de lecture du suivi : chronologie d'une session et statistiques
 * par variante.
 */
@RestController
@RequestMapping("/api")
public class SuiviController {

    private final SuiviService suiviService;

    public SuiviController(SuiviService suiviService) {
        this.suiviService = suiviService;
    }

    /** GET /api/sessions/{id}/chronologie → entrées triées par date croissante. */
    @GetMapping("/sessions/{id}/chronologie")
    public List<EntreeChronologie> chronologie(@PathVariable("id") Integer id) {
        return suiviService.chronologie(id);
    }

    /** GET /api/suivi/variantes?session_id= → statistiques par variante. */
    @GetMapping("/suivi/variantes")
    public List<StatVariante> statistiquesVariantes(
            @RequestParam(name = "session_id", required = false) Integer sessionId) {
        return suiviService.statistiquesVariantes(sessionId);
    }
}
