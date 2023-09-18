package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionTextEntity;
import za.co.mawa.bes.entity.transaction.TransactionTextPKEntity;

import java.util.List;

@Repository
public interface TransactionTextRepository extends JpaRepository<TransactionTextEntity, TransactionTextPKEntity> {
    @Query("SELECT t FROM TransactionTextEntity t WHERE t.transactionTextPKEntity.transaction = :transaction")
    List<TransactionTextEntity> getTransactionTexts(String transaction);

    @Query("SELECT t FROM TransactionTextEntity t WHERE t.transactionTextPKEntity.transaction = :transaction AND t.transactionTextPKEntity.type = :type")
    TransactionTextEntity getTransactionText(String transaction,String type);

}
