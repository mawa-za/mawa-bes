package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CaseTrustLedgerEntity;

import java.util.Optional;

public interface CaseTrustLedgerRepository extends JpaRepository<CaseTrustLedgerEntity, String> {

    Optional<CaseTrustLedgerEntity> findByCaseId(String caseId);
}
