package com.goungue.backend.repository;

import com.goungue.backend.model.Cohorte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CohorteRepository extends JpaRepository<Cohorte, Long> {
    List<Cohorte> findByFormateurId(Long formateurId);
}
