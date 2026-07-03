package com.goungue.backend.repository;

import com.goungue.backend.model.SessionFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionFormationRepository extends JpaRepository<SessionFormation, Long> {
    List<SessionFormation> findByFormateurId(Long formateurId);
}
