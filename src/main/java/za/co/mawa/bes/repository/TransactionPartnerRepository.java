package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionPartnerEntity;
import za.co.mawa.bes.entity.transaction.TransactionPartnerPKEntity;

import java.util.List;

@Repository
public interface TransactionPartnerRepository  extends JpaRepository<TransactionPartnerEntity, TransactionPartnerPKEntity> {
    @Query("SELECT t FROM TransactionPartnerEntity t WHERE t.transactionPartnerPKEntity.partner = :partner")
    List<TransactionPartnerEntity> findTransactionByPartner(String partner);

    @Query("SELECT t FROM TransactionPartnerEntity t WHERE t.transactionPartnerPKEntity.transaction = :transaction")
    List<TransactionPartnerEntity> findPartnerByTransaction(String transaction);
}
