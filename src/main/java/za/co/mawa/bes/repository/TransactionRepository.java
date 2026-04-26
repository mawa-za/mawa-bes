package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionEntity;

import java.util.List;

@Component
public interface TransactionRepository extends JpaRepository<TransactionEntity,String> {
    @Query(value = "SELECT * FROM transaction t WHERE t.type = :type", nativeQuery = true)
    List<TransactionEntity> findTransactionByType(String type);
    @Query(value = "SELECT * FROM transaction t WHERE t.status = :status", nativeQuery = true)
    List<TransactionEntity> findTransactionByStatus(String status);
    @Query(value = "SELECT * FROM transaction t WHERE t.sub_type = :subType", nativeQuery = true)
    List<TransactionEntity> findTransactionBySubType(String subType);
    @Query(value = "SELECT * FROM transaction t WHERE t.number = :number", nativeQuery = true)
    List<TransactionEntity> findTransactionByNumber(String number);
    @Query(value = "SELECT * FROM transaction t WHERE t.created_by = :createdBy AND t.type = :type", nativeQuery = true)
    List<TransactionEntity> findTransactionByCreatedBy(String createdBy,String type);
    @Query(value = "SELECT * FROM transaction t WHERE t.changed_by = :changedBy AND t.type = :type", nativeQuery = true)
    List<TransactionEntity> findTransactionByChangedBy(String changedBy,String type);
    @Query(value = "SELECT * FROM transaction t WHERE t.location = :location AND t.type = :type", nativeQuery = true)
    List<TransactionEntity> findTransactionByLocation(String location,String type);
    @Query(value = "SELECT * FROM transaction t WHERE t.type = :type AND t.number = :number limit 1", nativeQuery = true)
    TransactionEntity findTransactionByTypeNumber(String type, String number);

}
