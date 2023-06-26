package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionAttachmentEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttachmentPKEntity;
import java.util.List;


@Repository
public interface TransactionAttachmentRepository extends JpaRepository<TransactionAttachmentEntity, TransactionAttachmentPKEntity>{

    @Query("SELECT t FROM TransactionAttachmentEntity t WHERE t.transactionAttachmentPKEntity.transaction = :transaction")
    List<TransactionAttachmentEntity> findByTransaction(String transaction);
}
