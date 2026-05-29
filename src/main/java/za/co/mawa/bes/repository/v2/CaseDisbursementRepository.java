package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import java.util.List;

public interface CaseDisbursementRepository extends JpaRepository<CaseDisbursementEntity, String> {
    List<CaseDisbursementEntity> findByCaseIdOrderByDisbursementDateDesc(String caseId);
    List<CaseDisbursementEntity> findByCaseIdAndBilledFalse(String caseId);
}
