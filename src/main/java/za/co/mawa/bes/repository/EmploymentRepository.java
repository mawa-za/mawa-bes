package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.EmploymentEntity;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmploymentRepository extends JpaRepository<EmploymentEntity, String>, JpaSpecificationExecutor<EmploymentEntity> {
    boolean existsByPartnerIdAndStatus(String partnerId, String status);
    boolean existsByPartnerIdAndStatusIn(String partnerId, List<String> statuses);
    List<EmploymentEntity> findByPartnerIdAndStatus(String partnerId, String status);
    List<EmploymentEntity> findByPartnerIdOrderByStartDateDesc(String partnerId);
    Optional<EmploymentEntity> findFirstByPartnerIdAndStatusInOrderByStartDateDesc(String partnerId, List<String> statuses);
    Optional<EmploymentEntity> findFirstByEmployeeNumberOrderByStartDateDesc(String employeeNumber);

    @Query("select e from EmploymentEntity e where e.partnerId = :partnerId " +
           "and e.startDate <= :onDate and (e.endDate is null or e.endDate >= :onDate) " +
           "and e.status in :statuses order by e.startDate desc")
    List<EmploymentEntity> findApplicableEmployment(
            @Param("partnerId") String partnerId,
            @Param("onDate") Date onDate,
            @Param("statuses") List<String> statuses);
}
