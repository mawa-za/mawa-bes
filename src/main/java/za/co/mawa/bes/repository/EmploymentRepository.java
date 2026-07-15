package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.EmploymentEntity;

import java.util.List;

@Repository
public interface EmploymentRepository extends JpaRepository<EmploymentEntity, String>, JpaSpecificationExecutor<EmploymentEntity> {
    boolean existsByPartnerIdAndStatus(String partnerId, String status);
    List<EmploymentEntity> findByPartnerIdAndStatus(String partnerId, String status);
}
