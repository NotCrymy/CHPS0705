package fr.kapture.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.kapture.api.entity.Affichage;

/**
 * Repository des affichages de variantes (table {@code affichage}).
 */
public interface AffichageRepository extends JpaRepository<Affichage, Long> {

    /** Affichages encore ouverts ({@code fin} nulle) d'une session, à fermer. */
    List<Affichage> findBySession_IdAndFinIsNull(Integer sessionId);

    /** Dernier affichage d'une session : variante courante à défaut d'assignation en mémoire. */
    Optional<Affichage> findFirstBySession_IdOrderByDebutDescIdDesc(Integer sessionId);

    /** Chronologie d'une session (variante chargée d'avance pour le code). */
    @Query("""
            select a from Affichage a
            left join fetch a.variante
            where a.session.id = :sessionId
            """)
    List<Affichage> chronologie(@Param("sessionId") Integer sessionId);

    /** Suivi par variante : tous les affichages, ou ceux d'une session si {@code sessionId} non nul. */
    @Query("""
            select a from Affichage a
            left join fetch a.variante
            where (:sessionId is null or a.session.id = :sessionId)
            """)
    List<Affichage> pourSuivi(@Param("sessionId") Integer sessionId);
}
