package fr.kapture.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.kapture.api.entity.Variante;

/**
 * Repository du catalogue de variantes (table {@code variante}).
 */
public interface VarianteRepository extends JpaRepository<Variante, Integer> {

    /** Recherche par code ({@code NEUTRE} sert de variante par défaut). */
    Optional<Variante> findByCode(String code);

    /** Catalogue dans l'ordre d'insertion. */
    List<Variante> findAllByOrderByIdAsc();
}
