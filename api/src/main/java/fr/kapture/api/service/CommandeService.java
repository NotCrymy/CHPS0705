package fr.kapture.api.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.kapture.api.dto.CommandeRequest;
import fr.kapture.api.dto.CommandeResponse;
import fr.kapture.api.dto.EntreeChronologie;
import fr.kapture.api.dto.ResultatCommandeRequest;
import fr.kapture.api.dto.StatutCommande;
import fr.kapture.api.dto.TypeCommande;
import fr.kapture.api.entity.Commande;
import fr.kapture.api.entity.Session;
import fr.kapture.api.exception.ConflitException;
import fr.kapture.api.exception.RequeteInvalideException;
import fr.kapture.api.exception.RessourceIntrouvableException;
import fr.kapture.api.repository.CommandeRepository;
import tools.jackson.databind.JsonNode;

/**
 * Commandes envoyées au jeu par l'application (v2) : création (poussée aux
 * overlays de la session) et résultat rendu par l'overlay.
 *
 * <p>Ordre des contrôles à la création : session (404), type et paramètres
 * (400), overlay connecté (409).</p>
 */
@Service
@Transactional
public class CommandeService {

    private final ReferencesSession references;
    private final CommandeRepository commandes;
    private final FluxService flux;

    public CommandeService(ReferencesSession references, CommandeRepository commandes, FluxService flux) {
        this.references = references;
        this.commandes = commandes;
        this.flux = flux;
    }

    /** POST /api/sessions/{id}/commandes — crée la commande {@code ENVOYEE} et la pousse aux overlays. */
    public CommandeResponse creerCommande(Integer idSession, CommandeRequest requete) {
        Session session = references.session(idSession);
        if (!TypeCommande.estValide(requete.type())) {
            throw new RequeteInvalideException("Type de commande inconnu.");
        }
        JsonNode parametres = requete.parametres();
        JsonNode titre = (parametres == null || !parametres.isObject()) ? null : parametres.get("titre");
        if (titre == null || !titre.isString() || titre.asString().isBlank()) {
            throw new RequeteInvalideException("Le titre du bug est obligatoire.");
        }
        if (titre.asString().length() > 100) {
            throw new RequeteInvalideException("Le titre ne doit pas dépasser 100 caractères.");
        }
        if (flux.nombreOverlays(idSession) == 0) {
            throw new ConflitException("Aucun overlay connecté pour cette session.");
        }

        Commande commande = new Commande();
        commande.setSession(session);
        commande.setType(requete.type());
        commande.setParametres(parametres);
        commande.setStatut(StatutCommande.ENVOYEE.name());
        commande.setCreeA(maintenant());
        commandes.save(commande);

        CommandeResponse reponse = CommandeResponse.depuis(commande);
        flux.publierAuxOverlays(idSession, "commande", reponse);
        flux.publierChronologie(EntreeChronologie.commande(commande));
        return reponse;
    }

    /** POST /api/commandes/{id}/resultat — {@code EXECUTEE} ou {@code ECHEC}, une seule fois. */
    public CommandeResponse enregistrerResultat(Long idCommande, ResultatCommandeRequest requete) {
        Commande commande = commandes.findById(idCommande)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande inconnue."));
        if (!StatutCommande.ENVOYEE.name().equals(commande.getStatut())) {
            throw new ConflitException("Commande déjà traitée.");
        }
        commande.setStatut(requete.succes() ? StatutCommande.EXECUTEE.name() : StatutCommande.ECHEC.name());
        commande.setMessage(requete.message());
        commande.setTraiteA(maintenant());

        flux.publierChronologie(EntreeChronologie.resultatCommande(commande));
        return CommandeResponse.depuis(commande);
    }

    private static Instant maintenant() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
