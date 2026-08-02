package za.co.mawa.bes.repository.v2;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.LeaveBalanceAdjustmentRequestEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceAdjustmentRequestRepository extends JpaRepository<LeaveBalanceAdjustmentRequestEntity, String> {
    List<LeaveBalanceAdjustmentRequestEntity> findAllByOrderByRequestedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select adjustment from LeaveBalanceAdjustmentRequestEntity adjustment where adjustment.id = :id")
    Optional<LeaveBalanceAdjustmentRequestEntity> findByIdForUpdate(@Param("id") String id);
}
