package com.goungue.backend.repository;

import com.goungue.backend.model.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
    List<RendezVous> findByMentorId(Long mentorId);
    List<RendezVous> findByJeuneId(Long jeuneId);
}
