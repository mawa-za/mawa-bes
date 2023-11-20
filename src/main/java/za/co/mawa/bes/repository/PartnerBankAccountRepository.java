package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.PartnerBankAccountEntity;
import za.co.mawa.bes.entity.PartnerBankingDetailsEntity;
import za.co.mawa.bes.entity.PartnerBankingDetailsPKEntity;

import java.util.List;

public interface PartnerBankAccountRepository extends JpaRepository<PartnerBankAccountEntity, String> {
    @Query("SELECT b FROM PartnerBankAccountEntity b WHERE b.partner = :partner")
    List<PartnerBankAccountEntity> findPartnerBankByPartner(String partner);


}
