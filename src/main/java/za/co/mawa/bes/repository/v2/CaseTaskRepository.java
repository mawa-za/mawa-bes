package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CaseTaskEntity;
import za.co.mawa.bes.enums.CaseTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface CaseTaskRepository extends JpaRepository<CaseTaskEntity, String> {

    List<CaseTaskEntity> findByCaseIdOrderByDueDateAsc(String caseId);

    List<CaseTaskEntity> findByAssignedToOrderByDueDateAsc(String assignedTo);

    List<CaseTaskEntity> findByAssignedToAndStatusOrderByDueDateAsc(String assignedTo, CaseTaskStatus status);

    List<CaseTaskEntity> findByStatusNotAndDueDateBeforeOrderByDueDateAsc(CaseTaskStatus status, LocalDateTime dueDate);
}
