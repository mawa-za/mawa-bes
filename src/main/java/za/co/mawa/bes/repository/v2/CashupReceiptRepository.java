package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CashupReceiptEntity;

import java.util.List;

public interface CashupReceiptRepository extends JpaRepository<CashupReceiptEntity, String> {

    List<CashupReceiptEntity> findByCashupId(String cashupId);

    boolean existsByCashupIdAndReceiptId(String cashupId, String receiptId);

    void deleteByCashupId(String cashupId);
}
