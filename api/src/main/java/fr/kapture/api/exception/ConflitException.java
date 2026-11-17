package fr.kapture.api.exception;

/**
 * Requête valide mais incompatible avec l'état courant → réponse {@code 409}
 * (ex. « Aucun overlay connecté pour cette session. », « Commande déjà
 * traitée. »). Le message est celui affiché au client (contrat
 * {@code docs/V2.md}).
 */
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}
