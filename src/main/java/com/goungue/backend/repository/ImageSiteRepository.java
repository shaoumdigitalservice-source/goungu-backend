package com.goungue.backend.repository;

import com.goungue.backend.model.ImageSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageSiteRepository extends JpaRepository<ImageSite, Long> {
    Optional<ImageSite> findByCle(String cle);
}