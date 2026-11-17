package fr.kapture.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.kapture.api.dto.AssignationVarianteRequest;
import fr.kapture.api.dto.SessionEtat;
import fr.kapture.api.dto.VarianteResponse;
import fr.kapture.api.service.SessionService;
import jakarta.validation.Valid;

/**
 * Routes v2 de pilotage ({@code docs/V2.md} §5.3) : catalogue de variantes,
 * état en direct des sessions, assignation d'une variante.
 */
@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** GET /api/variantes → catalogue. */
    @GetMapping("/variantes")
    public List<VarianteResponse> listerVariantes() {
        return sessionService.listerVariantes();
    }

    /** GET /api/sessions → sessions + état en direct. */
    @GetMapping("/sessions")
    public List<SessionEtat> listerSessions() {
        return sessionService.listerSessions();
    }

    /** PUT /api/sessions/{id}/variante → 200 + SessionEtat (config validée par Jakarta). */
    @PutMapping("/sessions/{id}/variante")
    public SessionEtat assignerVariante(
            @PathVariable("id") Integer id,
            @Valid @RequestBody AssignationVarianteRequest requete) {
        return sessionService.assignerVariante(id, requete);
    }
}
