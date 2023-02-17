package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionAmountPKEntity;

import java.util.List;

@Repository
public interface TransactionAmountRepository extends JpaRepository<TransactionAmountEntity, TransactionAmountPKEntity> {
    @Query("SELECT t FROM TransactionAmountEntity t WHERE t.transactionAmountPKEntity.transaction = :transaction")
    List<TransactionAmountEntity> getTransactionAmounts(String transaction);
}
