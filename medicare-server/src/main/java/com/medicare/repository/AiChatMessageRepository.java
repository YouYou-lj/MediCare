package com.medicare.repository;

import com.medicare.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findBySessionIdOrderByCreateTimeAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
