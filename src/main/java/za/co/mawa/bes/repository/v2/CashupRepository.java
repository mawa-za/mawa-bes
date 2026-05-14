package za.co.mawa.bes.repository.v2;


import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CashupEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashupRepository extends JpaRepository<CashupEntity, String> {

    Optional<CashupEntity> findByCashupNo(Long cashupNo);

    boolean existsByCashupNo(Long cashupNo);

    List<CashupEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<CashupEntity> findByUserIdAndCashupDateBetweenOrderByCashupDateDesc(
            String userId,
            LocalDate fromDate,
            LocalDate toDate
    );
}
