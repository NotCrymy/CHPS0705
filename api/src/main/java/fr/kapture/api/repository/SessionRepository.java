package fr.kapture.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fr.kapture.api.entity.Session;

/**
 * Repository des sessions de jeu (table {@code session}). Sert à valider le
 * {@code id} de session d'un bug ({@code existsById} hérité → « Session
 * inconnue. » en 404).
 *
 * <p>v2 : {@link #toutesAvecUtilisateur()} alimente {@code GET /api/sessions}.</p>
 */
public interface SessionRepository extends JpaRepository<Session, Integer> {

    /** Toutes les sessions avec leur pseudonyme (évite le N+1), triées par id. */
    @Query("select s from Session s join fetch s.utilisateur order by s.id")
    List<Session> toutesAvecUtilisateur();
}
