package za.co.mawa.bes.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkPKEntity;

import java.util.List;
@Repository
public interface TransactionLinkRepository extends JpaRepository<TransactionLinkEntity, TransactionLinkPKEntity> {

    @Query("SELECT t FROM TransactionLinkEntity t WHERE t.transactionLinkPKEntity.transaction1 = :transaction1")
    List<TransactionLinkEntity> getTransactionLinks(String transaction1);

}
