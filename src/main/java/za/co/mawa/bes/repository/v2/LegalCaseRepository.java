package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.enums.LegalCaseStatus;

import java.util.List;

public interface LegalCaseRepository extends JpaRepository<LegalCaseEntity, String> {

    List<LegalCaseEntity> findByClientPartnerIdOrderByOpenedDateDesc(String clientPartnerId);

    List<LegalCaseEntity> findByAssignedToOrderByOpenedDateDesc(String assignedTo);

    List<LegalCaseEntity> findByStatusOrderByOpenedDateDesc(LegalCaseStatus status);

    long countByStatus(LegalCaseStatus status);

    boolean existsByCaseNo(String caseNo);
}
