package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemPKEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItemEntity, TransactionItemPKEntity> {
    @Query(value = "SELECT * FROM transaction_item " +
            "WHERE transaction = :transaction AND product IS NOT NULL AND TRIM(product) <> '' " +
            "ORDER BY valid_from DESC, item DESC LIMIT 1", nativeQuery = true)
    Optional<TransactionItemEntity> findCurrentMembershipItem(String transaction);
    @Query("SELECT t FROM TransactionItemEntity t WHERE t.transactionItemPKEntity.transaction = :transaction")
    List<TransactionItemEntity> getTransactionItems(String transaction);
    @Query("SELECT t FROM TransactionItemEntity t WHERE t.transactionItemPKEntity.transaction = :transaction AND product = :product")
    TransactionItemEntity getTransactionItem(String transaction,String product);
    @Query("SELECT t FROM TransactionItemEntity t WHERE t.product = :product")
    List<TransactionItemEntity> findItemBy(String product);

}
