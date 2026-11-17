package fr.kapture.api.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.kapture.api.dto.AffichageRequest;
import fr.kapture.api.dto.ApparitionRequest;
import fr.kapture.api.dto.InteractionRequest;
import fr.kapture.api.service.CollecteService;
import jakarta.validation.Valid;

/**
 * Routes v2 de collecte appelées par l'overlay : affichages, apparitions,
 * interactions. Chacune répond {@code 201 { "id": … }}.
 */
@RestController
@RequestMapping("/api/sessions/{id}")
public class CollecteController {

    private final CollecteService collecteService;

    public CollecteController(CollecteService collecteService) {
        this.collecteService = collecteService;
    }

    /** POST /api/sessions/{id}/affichages. */
    @PostMapping("/affichages")
    public ResponseEntity<Map<String, Long>> creerAffichage(
            @PathVariable("id") Integer id,
            @Valid @RequestBody AffichageRequest requete) {
        return cree(collecteService.creerAffichage(id, requete));
    }

    /** POST /api/sessions/{id}/apparitions. */
    @PostMapping("/apparitions")
    public ResponseEntity<Map<String, Long>> creerApparition(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ApparitionRequest requete) {
        return cree(collecteService.creerApparition(id, requete));
    }

    /** POST /api/sessions/{id}/interactions. */
    @PostMapping("/interactions")
    public ResponseEntity<Map<String, Long>> creerInteraction(
            @PathVariable("id") Integer id,
            @Valid @RequestBody InteractionRequest requete) {
        return cree(collecteService.creerInteraction(id, requete));
    }

    private static ResponseEntity<Map<String, Long>> cree(Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }
}
