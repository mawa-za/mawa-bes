package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.List;

@Component
public interface TransactionRepository extends JpaRepository<TransactionEntity,String> {
    @Query(value = "SELECT t FROM Transaction t WHERE t.type = :type", nativeQuery = true)
    List<TransactionEntity> findTransactionByType(String type);

    @Query(value = "SELECT t FROM Transaction t WHERE t.status = :status", nativeQuery = true)
    List<TransactionEntity> findTransactionByStatus(String status);

}
