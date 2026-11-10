package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CashupDepositEntity;

import java.util.List;

public interface CashupDepositRepository extends JpaRepository<CashupDepositEntity, String> {

    List<CashupDepositEntity> findByCashupIdOrderByDepositDateDescCreatedAtDesc(String cashupId);

    @org.springframework.data.jpa.repository.Query("select d from CashupDepositEntity d where d.bankDepositId is null "
            + "and (:query = '' or lower(coalesce(d.referenceNo,'')) like lower(concat('%',:query,'%'))) "
            + "order by d.createdAt desc")
    org.springframework.data.domain.Page<CashupDepositEntity> pendingBankVerification(
            @org.springframework.data.repository.query.Param("query") String query,
            org.springframework.data.domain.Pageable page);

    long countByCashupId(String cashupId);
}
