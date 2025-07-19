package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "print_job")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrintJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creation_timestamp",updatable = false)
    private Timestamp creationTimestamp;

    @Column(name = "printer_id", nullable = false)
    private String printerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_timestamp")
    private Timestamp completedTimestamp;
    @PrePersist
    private void setTimestamp(){
        this.creationTimestamp = new Timestamp(System.currentTimeMillis());
    }
}
