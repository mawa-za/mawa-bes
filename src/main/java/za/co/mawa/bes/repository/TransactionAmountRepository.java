package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import java.util.List;

@Repository
public interface TransactionAmountRepository extends JpaRepository<TransactionAmountEntity, String> {
    @Query("SELECT t FROM TransactionAmountEntity t WHERE t.transaction = :transaction")
    List<TransactionAmountEntity> getByTransaction(String transaction);

}
