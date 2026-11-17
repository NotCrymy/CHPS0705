package fr.kapture.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.kapture.api.entity.Interaction;

/**
 * Repository des interactions du joueur avec le bouton (table
 * {@code interaction}).
 */
public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    /** Chronologie d'une session. */
    List<Interaction> findBySession_Id(Integer sessionId);

    /**
     * Suivi : couples {@code [affichage_id (Long), type (String)]} des
     * interactions rattachées à un affichage, filtrables par session.
     */
    @Query("""
            select af.id, i.type from Interaction i
            join i.affichage af
            where (:sessionId is null or i.session.id = :sessionId)
            """)
    List<Object[]> affichagesEtTypes(@Param("sessionId") Integer sessionId);
}
