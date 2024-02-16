package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.BankAccountEntity;
import za.co.mawa.bes.entity.PartnerBankAccountEntity;
import za.co.mawa.bes.entity.transaction.TransactionBankAccount;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccountEntity, String> {
    @Query("SELECT b FROM BankAccountEntity b WHERE b.objectId = :objectId")
    BankAccountEntity getByObjectId(String objectId);
}
