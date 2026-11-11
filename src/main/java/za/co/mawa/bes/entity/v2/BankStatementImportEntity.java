package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "bank_statement_import") @Getter @Setter
public class BankStatementImportEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 20) private String provider;
    @Column(name = "message_id", nullable = false) private String messageId;
    @Column(name = "attachment_id", nullable = false) private String attachmentId;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(nullable = false, length = 64) private String sha256;
    @Column(name = "source_file_id") private Long sourceFileId;
    @Column(name = "imported_count", nullable = false) private Integer importedCount = 0;
    @Column(name = "imported_at", nullable = false) private LocalDateTime importedAt = LocalDateTime.now();
}
