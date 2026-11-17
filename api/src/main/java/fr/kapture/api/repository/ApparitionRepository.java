package fr.kapture.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.kapture.api.entity.Apparition;

/**
 * Repository des apparitions de bugs dans le jeu (table {@code apparition}).
 */
public interface ApparitionRepository extends JpaRepository<Apparition, Long> {

    /** Chronologie d'une session. */
    List<Apparition> findBySession_Id(Integer sessionId);

    /**
     * Suivi : {@code affichage_id} de chaque apparition rattachée à un
     * affichage (une ligne par apparition), filtrable par session.
     */
    @Query("""
            select af.id from Apparition a
            join a.affichage af
            where (:sessionId is null or a.session.id = :sessionId)
            """)
    List<Long> affichagesDesApparitions(@Param("sessionId") Integer sessionId);
}
