package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributePKEntity;

import java.util.List;

@Repository
public interface TransactionAttributeRepository extends JpaRepository<TransactionAttributeEntity, TransactionAttributePKEntity> {
    List<TransactionAttributeEntity> findAll(Specification<TransactionAttributeEntity> byCriteria, Sort sort);
}
