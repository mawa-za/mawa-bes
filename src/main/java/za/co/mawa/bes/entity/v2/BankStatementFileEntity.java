package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** One immutable original file, retained in tenant-scoped GCS for audit and safe replay. */
@Entity @Table(name = "bank_statement_file") @Getter @Setter
public class BankStatementFileEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 12) private String source;
    @Column(nullable = false, length = 20) private String provider;
    @Column(name = "message_id") private String messageId;
    @Column(name = "attachment_id") private String attachmentId;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(nullable = false, length = 64) private String sha256;
    @Column(name = "byte_size", nullable = false) private Long byteSize;
    @Column(name = "storage_bucket", length = 255) private String storageBucket;
    @Column(name = "storage_path", length = 900) private String storagePath;
    @Column(name = "uploaded_by", nullable = false, length = 255) private String uploadedBy;
    @Column(nullable = false, length = 24) private String status = "RECEIVED";
    @Column(name = "imported_count", nullable = false) private Integer importedCount = 0;
    @Column(name = "csv_count", nullable = false) private Integer csvCount = 0;
    @Column(name = "failed_csv_count", nullable = false) private Integer failedCsvCount = 0;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount = 0;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "last_processed_at") private LocalDateTime lastProcessedAt;
}
