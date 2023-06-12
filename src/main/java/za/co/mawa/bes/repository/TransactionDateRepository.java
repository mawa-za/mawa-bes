package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionDateEntity;
import za.co.mawa.bes.entity.transaction.TransactionDatePKEntity;

import java.util.Date;
import java.util.List;

@Repository
public interface TransactionDateRepository extends JpaRepository<TransactionDateEntity, TransactionDatePKEntity> {
    @Query("SELECT t FROM TransactionDateEntity t WHERE t.transactionDatePKEntity.transaction = :transaction")
    List<TransactionDateEntity> getTransactionDates(String transaction);

    @Query("SELECT t FROM TransactionDateEntity t WHERE t.transactionDatePKEntity.transaction = :transaction AND t.transactionDatePKEntity.type = :type")
    TransactionDateEntity getTransactionDatesType(String transaction,String type);
    @Query("SELECT t FROM TransactionDateEntity t WHERE t.transactionDatePKEntity.type = :dateType AND DATE(t.value) = DATE(:value)")
    List<TransactionDateEntity> findByDateType(Date value,String dateType);

}
