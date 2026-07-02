package com.goungue.backend.repository;

import com.goungue.backend.model.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, Long> {
    List<Programme> findByActifTrueOrderByOrdreAffichageAsc();
    Optional<Programme> findBySlug(String slug);
}