package za.co.mawa.bes.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.EmploymentEntity;

import java.util.List;

@Repository
public interface EmploymentRepository extends JpaRepository<EmploymentEntity,String> {
    List<EmploymentEntity> findAll(Specification<EmploymentEntity> byCriteria, Sort sort);

}
