package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.PartnerBankingDetailsEntity;
import za.co.mawa.bes.entity.PartnerBankingDetailsPKEntity;

import java.util.List;

public interface PartnerBankAccountRepository extends JpaRepository<PartnerBankingDetailsEntity, PartnerBankingDetailsPKEntity> {
    @Query("SELECT b FROM PartnerBankingDetailsEntity b WHERE b.partnerBankingDetailsPk.partner = :partner")
    List<PartnerBankingDetailsEntity> findPartnerBankByPartner(String partner);
}
