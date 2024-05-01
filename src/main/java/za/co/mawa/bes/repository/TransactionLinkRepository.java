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
    @Query("SELECT t FROM TransactionLinkEntity t WHERE t.transactionLinkPKEntity.transaction2 = :transaction2 AND t.transactionLinkPKEntity.type = :type" )
    TransactionLinkEntity getTransactionLinks(String transaction2,String type);
    @Query("SELECT t FROM TransactionLinkEntity t WHERE t.transactionLinkPKEntity.transaction2 = :transaction2 AND t.transactionLinkPKEntity.type = :type" )
    List<TransactionLinkEntity> getTransactionLink(String transaction2,String type);
    @Query("SELECT t FROM TransactionLinkEntity t WHERE t.transactionLinkPKEntity.transaction1 = :parent")
    List<TransactionLinkEntity> getChildren(String parent);
    @Query("SELECT t FROM TransactionLinkEntity t WHERE t.transactionLinkPKEntity.transaction2 = :child")
    List<TransactionLinkEntity> getParent(String child);
    @Query("SELECT t FROM TransactionLinkEntity t WHERE t.transactionLinkPKEntity.transaction2 = :transaction2 AND t.transactionLinkPKEntity.type = :type" )
    TransactionLinkEntity getLink(String child,String type);

}
