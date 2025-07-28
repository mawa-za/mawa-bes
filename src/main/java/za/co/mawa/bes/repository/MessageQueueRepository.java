package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.MessageQueueEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageQueueRepository  extends JpaRepository<MessageQueueEntity, Long> {
    List<MessageQueueEntity> findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(LocalDateTime now);
}
