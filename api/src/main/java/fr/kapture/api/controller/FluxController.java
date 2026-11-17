package fr.kapture.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import fr.kapture.api.service.FluxService;
import fr.kapture.api.service.SessionService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Flux Server-Sent Events v2 : overlay d'une session et console.
 *
 * <p>{@code X-Accel-Buffering: no} demande à Nginx de ne pas bufferiser le
 * flux (en plus de {@code proxy_buffering off}).</p>
 */
@RestController
@RequestMapping("/api")
public class FluxController {

    private final SessionService sessionService;
    private final FluxService fluxService;

    public FluxController(SessionService sessionService, FluxService fluxService) {
        this.sessionService = sessionService;
        this.fluxService = fluxService;
    }

    /** GET /api/sessions/{id}/flux → SSE overlay ({@code variante}, {@code commande}) ; 404 si session inconnue. */
    @GetMapping("/sessions/{id}/flux")
    public SseEmitter fluxOverlay(@PathVariable("id") Integer id, HttpServletResponse reponse) {
        SseEmitter emetteur = sessionService.ouvrirFluxOverlay(id);
        sansTampon(reponse);
        return emetteur;
    }

    /** GET /api/flux → SSE console ({@code chronologie}). */
    @GetMapping("/flux")
    public SseEmitter fluxConsole(HttpServletResponse reponse) {
        sansTampon(reponse);
        return fluxService.abonnerConsole();
    }

    private static void sansTampon(HttpServletResponse reponse) {
        reponse.setHeader("X-Accel-Buffering", "no");
        reponse.setHeader("Cache-Control", "no-cache");
    }
}
