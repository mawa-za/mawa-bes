package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.BankStatementImportEntity;

public interface BankStatementImportRepository extends JpaRepository<BankStatementImportEntity, Long> {
    boolean existsByProviderAndMessageIdAndAttachmentId(String provider, String messageId, String attachmentId);
}
