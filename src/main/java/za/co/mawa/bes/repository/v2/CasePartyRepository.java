package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CasePartyEntity;
import za.co.mawa.bes.enums.CasePartyType;

import java.util.List;

public interface CasePartyRepository extends JpaRepository<CasePartyEntity, String> {

    List<CasePartyEntity> findByCaseIdOrderByPartyNameAsc(String caseId);

    List<CasePartyEntity> findByCaseIdAndPartyTypeOrderByPartyNameAsc(String caseId, CasePartyType partyType);
}
