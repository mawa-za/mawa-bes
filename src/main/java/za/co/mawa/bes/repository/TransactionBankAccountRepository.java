package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.transaction.TransactionBankAccount;

@Repository
public interface TransactionBankAccountRepository extends JpaRepository<TransactionBankAccount,String>{
    @Query("SELECT t FROM TransactionBankAccount t WHERE t.transaction = :id ")
    TransactionBankAccount getBankAccount(String id);
}
