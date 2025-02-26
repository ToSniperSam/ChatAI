package org.example.chatai.service;

import org.example.chatai.common.ChatRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRecordRepository extends JpaRepository<ChatRecord, Long> {
}
