package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CashupDepositEntity;

import java.util.List;

public interface CashupDepositRepository extends JpaRepository<CashupDepositEntity, String> {

    List<CashupDepositEntity> findByCashupIdOrderByDepositDateDescCreatedAtDesc(String cashupId);

    long countByCashupId(String cashupId);
}
