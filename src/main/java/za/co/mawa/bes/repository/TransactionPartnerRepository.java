package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.TransactionPartnerEntity;
import za.co.mawa.bes.entity.TransactionPartnerPKEntity;

import java.util.List;

@Repository
public interface TransactionPartnerRepository  extends JpaRepository<TransactionPartnerEntity, TransactionPartnerPKEntity> {
    @Query(value = "SELECT t FROM TransactionPartner t WHERE t.transactionPartnerPK.partnerNo = :partnerNo", nativeQuery = true)
    List<TransactionPartnerEntity> findTransactionByPartner(String partner);

    @Query(value = "SELECT t FROM TransactionPartner t WHERE t.transactionPartnerPK.transactionId = :transactionId", nativeQuery = true)
    List<TransactionPartnerEntity> findPartnerByTransaction(String transaction);
}
