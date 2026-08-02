package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.LeaveRequestEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, String>, JpaSpecificationExecutor<LeaveRequestEntity> {
    @Query("select l from LeaveRequestEntity l where l.employmentId = :employmentId " +
            "and l.status in :statuses and l.startDate <= :endDate and l.endDate >= :startDate " +
            "and (:excludeId is null or l.id <> :excludeId)")
    List<LeaveRequestEntity> findOverlapping(
            @Param("employmentId") String employmentId,
            @Param("statuses") List<String> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") String excludeId);

    boolean existsByApprovalRequestId(String approvalRequestId);
}
