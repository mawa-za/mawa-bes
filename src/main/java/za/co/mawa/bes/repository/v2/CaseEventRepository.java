package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CaseEventEntity;
import za.co.mawa.bes.enums.CaseEventStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface CaseEventRepository extends JpaRepository<CaseEventEntity, String> {

    List<CaseEventEntity> findByCaseIdOrderByStartAtAsc(String caseId);

    List<CaseEventEntity> findByStartAtBetweenOrderByStartAtAsc(LocalDateTime from, LocalDateTime to);

    List<CaseEventEntity> findByStatusAndStartAtBetweenOrderByStartAtAsc(
            CaseEventStatus status,
            LocalDateTime from,
            LocalDateTime to
    );
}
