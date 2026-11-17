package fr.kapture.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.kapture.api.entity.Commande;

/**
 * Repository des commandes envoyées au jeu (table {@code commande}).
 */
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    /** Chronologie d'une session. */
    List<Commande> findBySession_Id(Integer sessionId);
}
