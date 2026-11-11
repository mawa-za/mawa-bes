package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** Append-only processing outcomes (never delete history when retrying). */
@Entity @Table(name = "bank_statement_file_attempt") @Getter @Setter
public class BankStatementFileAttemptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "file_id", nullable = false) private Long fileId;
    @Column(name = "attempt_number", nullable = false) private Integer attemptNumber;
    @Column(name = "attempted_by", nullable = false, length = 255) private String attemptedBy;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt = LocalDateTime.now();
    @Column(name = "finished_at") private LocalDateTime finishedAt;
    @Column(nullable = false, length = 24) private String status = "PROCESSING";
    @Column(name = "imported_count", nullable = false) private Integer importedCount = 0;
    @Column(name = "csv_count", nullable = false) private Integer csvCount = 0;
    @Column(name = "failed_csv_count", nullable = false) private Integer failedCsvCount = 0;
    @Column(length = 500) private String summary;
}
