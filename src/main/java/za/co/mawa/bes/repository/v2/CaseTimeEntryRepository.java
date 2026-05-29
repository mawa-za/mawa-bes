package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import java.util.List;

public interface CaseTimeEntryRepository extends JpaRepository<CaseTimeEntryEntity, String> {
    List<CaseTimeEntryEntity> findByCaseIdOrderByEntryDateDesc(String caseId);
    List<CaseTimeEntryEntity> findByCaseIdAndBilledFalse(String caseId);
}
