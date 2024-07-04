package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributePKEntity;

import java.util.List;

@Repository
public interface TransactionAttributeRepository extends JpaRepository<TransactionAttributeEntity, String> {
    @Query("SELECT p FROM TransactionAttributeEntity p WHERE p.transaction = :transaction AND p.attribute = :attribute")
    List<TransactionAttributeEntity> find(String transaction, String attribute);
    List<TransactionAttributeEntity> findAll(Specification<TransactionAttributeEntity> byCriteria, Sort sort);

    List<TransactionAttributeEntity> findAllByTransaction(String transactionId);
}
