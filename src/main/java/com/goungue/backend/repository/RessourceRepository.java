package com.goungue.backend.repository;

import com.goungue.backend.model.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RessourceRepository extends JpaRepository<Ressource, Long> {
    List<Ressource> findByActifTrueOrderByOrdreAffichageAsc();
}
