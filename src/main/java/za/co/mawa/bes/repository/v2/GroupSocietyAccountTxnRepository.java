package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;

import java.util.List;

public interface GroupSocietyAccountTxnRepository extends JpaRepository<GroupSocietyAccountTxnEntity, String> {

    List<GroupSocietyAccountTxnEntity> findByGroupSocietyIdOrderByTxnDatetimeDesc(String groupSocietyId);

    List<GroupSocietyAccountTxnEntity> findByGroupSocietyIdAndPeriodOrderByTxnDatetimeDesc(
            String groupSocietyId,
            String period
    );

    List<GroupSocietyAccountTxnEntity> findByReferenceTypeAndReferenceId(
            String referenceType,
            String referenceId
    );

    boolean existsByReferenceTypeAndReferenceIdAndTxnType(
            String referenceType,
            String referenceId,
            String txnType
    );
}