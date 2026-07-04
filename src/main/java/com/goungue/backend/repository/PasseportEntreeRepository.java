package com.goungue.backend.repository;

import com.goungue.backend.model.PasseportEntree;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasseportEntreeRepository extends JpaRepository<PasseportEntree, Long> {
    List<PasseportEntree> findByJeuneIdOrderByCreatedAtAsc(Long jeuneId);
}
