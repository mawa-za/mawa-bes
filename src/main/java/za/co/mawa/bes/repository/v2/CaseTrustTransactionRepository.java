package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CaseTrustTransactionEntity;

import java.util.List;

public interface CaseTrustTransactionRepository extends JpaRepository<CaseTrustTransactionEntity, String> {

    List<CaseTrustTransactionEntity> findByCaseIdOrderByTransactionDateDesc(String caseId);

    boolean existsByTransactionNo(String transactionNo);
}
