package com.goungue.backend.repository;

import com.goungue.backend.model.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvenementRepository extends JpaRepository<Evenement, Long> {
    List<Evenement> findByActifTrueOrderByDateEvenementAsc();
}
