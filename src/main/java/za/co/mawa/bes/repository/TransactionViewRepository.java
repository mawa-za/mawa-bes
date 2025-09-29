package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;


import java.util.List;

@Repository
public interface TransactionViewRepository extends JpaRepository<TransactionViewEntity, String> {
    @Query(value = "SELECT * FROM transaction_view WHERE transaction_id = :id", nativeQuery = true)
    List<TransactionViewEntity> findByTransactionId(@Param("id") String id);

    @Query(value = "SELECT * FROM transaction_view WHERE transaction_type = :type", nativeQuery = true)
    List<TransactionViewEntity> findAllByType(@Param("type") String type);

    @Query(value = "SELECT TOP 1 * FROM transaction_view WHERE transaction_type = :type and transaction_number = :number", nativeQuery = true)
    TransactionViewEntity findByNumber(@Param("type") String type, @Param("number") String number);

}
