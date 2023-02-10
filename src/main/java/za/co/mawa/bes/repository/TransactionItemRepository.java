package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemPKEntity;

import java.util.List;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItemEntity, TransactionItemPKEntity> {
    @Query(value = "SELECT t FROM TransactionItemEntity t WHERE t.transactionItemPKEntity.transaction = :transaction", nativeQuery = true)
    List<TransactionItemEntity> getTransactionItems(String transaction);
}
