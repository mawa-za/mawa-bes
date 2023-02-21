package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.EmploymentPKEntity;

import java.util.List;

@Repository
public interface EmploymentRepository extends JpaRepository<EmploymentEntity, EmploymentPKEntity> {
    @Query("SELECT e FROM EmploymentEntity e WHERE e.employmentPK.employeeId = :employeeId")
 List<EmploymentEntity> findEmploymentById(@Param("employeeId") String employeeId);

}
