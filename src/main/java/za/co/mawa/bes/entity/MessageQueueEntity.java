package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "message_queue")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class MessageQueueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "message_type")
    private String type;
    private String payload;
    private boolean processed = false;
    @Column(name = "retry_count")
    private int retryCount = 0;
    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt = LocalDateTime.now();
}
