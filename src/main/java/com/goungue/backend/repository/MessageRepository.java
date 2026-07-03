package com.goungue.backend.repository;

import com.goungue.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.expediteurId = :userA AND m.destinataireId = :userB) OR " +
           "(m.expediteurId = :userB AND m.destinataireId = :userA) " +
           "ORDER BY m.dateEnvoi ASC")
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);
}
