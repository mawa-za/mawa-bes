package za.co.mawa.bes.repository.v2;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.EmploymentActionRequestEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmploymentActionRequestRepository extends JpaRepository<EmploymentActionRequestEntity, String> {
    List<EmploymentActionRequestEntity> findAllByOrderByRequestedAtDesc();
    boolean existsByPartnerIdAndStatusIn(String partnerId, List<String> statuses);
    boolean existsByEmploymentIdAndStatusIn(String employmentId, List<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select action from EmploymentActionRequestEntity action where action.id = :id")
    Optional<EmploymentActionRequestEntity> findByIdForUpdate(@Param("id") String id);
}
