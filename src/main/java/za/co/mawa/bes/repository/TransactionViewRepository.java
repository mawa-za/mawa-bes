package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;


import java.util.List;

@Repository
public interface TransactionViewRepository extends JpaRepository<TransactionViewEntity, String> {
    @Query(value = "SELECT transaction_id, transaction_number, transaction_type, creation_date, transaction_type_desc, transaction_status, customer_identity_number, customer_name, employee_responsible, filter " +
            "FROM transaction_view WHERE transaction_id = :id", nativeQuery = true)
    List<TransactionViewEntity> findByTransactionId(String id);
    @Query(value = "SELECT transaction_id, transaction_number, transaction_type, creation_date, transaction_type_desc, transaction_status, customer_identity_number, customer_name, employee_responsible, filter " +
            "FROM transaction_view WHERE transaction_type = :type", nativeQuery = true)
    List<TransactionViewEntity> findAllByType(String type);
}
