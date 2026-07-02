package com.goungue.backend.repository;

import com.goungue.backend.model.MessageContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageContactRepository extends JpaRepository<MessageContact, Long> {
}