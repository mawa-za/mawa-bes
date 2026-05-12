package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;

import java.util.List;

public interface CashupPaymentSummaryRepository extends JpaRepository<CashupPaymentSummaryEntity, String> {

    List<CashupPaymentSummaryEntity> findByCashupId(String cashupId);
}
