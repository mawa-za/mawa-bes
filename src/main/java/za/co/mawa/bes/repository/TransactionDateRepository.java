package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.TransactionDateEntity;
import za.co.mawa.bes.entity.TransactionDatePKEntity;
import za.co.mawa.bes.entity.TransactionPartnerEntity;

import java.util.List;

@Repository
public interface TransactionDateRepository extends JpaRepository<TransactionDateEntity, TransactionDatePKEntity> {
    @Query(value = "SELECT t FROM TransactionDate t WHERE t.transactionDatePK.transaction = :transaction", nativeQuery = true)
    List<TransactionDateEntity> findTransactionByDate(String transaction);

}
