package com.medicare.repository;

import com.medicare.entity.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

    Optional<AiChatSession> findByUserIdAndSessionKey(Long userId, String sessionKey);

    List<AiChatSession> findByUserIdOrderByUpdateTimeDesc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query(value = "UPDATE ai_chat_session SET update_time = NOW(3) WHERE id = ?1", nativeQuery = true)
    void touchUpdateTime(Long id);
}
