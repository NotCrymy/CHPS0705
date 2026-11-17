package fr.kapture.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.kapture.api.dto.CommandeRequest;
import fr.kapture.api.dto.CommandeResponse;
import fr.kapture.api.dto.ResultatCommandeRequest;
import fr.kapture.api.service.CommandeService;
import jakarta.validation.Valid;

/**
 * Routes v2 des commandes envoyées au jeu : création (201, 409 sans overlay)
 * et résultat rendu par l'overlay (409 si déjà traitée).
 */
@RestController
@RequestMapping("/api")
public class CommandeController {

    private final CommandeService commandeService;

    public CommandeController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    /** POST /api/sessions/{id}/commandes → 201 + Commande. */
    @PostMapping("/sessions/{id}/commandes")
    public ResponseEntity<CommandeResponse> creerCommande(
            @PathVariable("id") Integer id,
            @RequestBody CommandeRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandeService.creerCommande(id, requete));
    }

    /** POST /api/commandes/{id}/resultat → 200 + Commande. */
    @PostMapping("/commandes/{id}/resultat")
    public CommandeResponse enregistrerResultat(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResultatCommandeRequest requete) {
        return commandeService.enregistrerResultat(id, requete);
    }
}
