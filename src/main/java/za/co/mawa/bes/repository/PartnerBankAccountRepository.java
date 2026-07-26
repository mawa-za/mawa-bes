package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.PartnerBankAccountEntity;

import java.util.List;

public interface PartnerBankAccountRepository extends JpaRepository<PartnerBankAccountEntity, String> {
    @Query("SELECT b FROM PartnerBankAccountEntity b WHERE b.partner = :partner ORDER BY b.validFrom DESC")
    List<PartnerBankAccountEntity> findByPartner(String partner);

    @Query("SELECT b FROM PartnerBankAccountEntity b WHERE b.partner = :partner AND b.status = :status ORDER BY b.validFrom DESC")
    List<PartnerBankAccountEntity> findByPartnerAndStatus(String partner, String status);
}
