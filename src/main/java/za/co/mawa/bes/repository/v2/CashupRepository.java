package za.co.mawa.bes.repository.v2;


import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import za.co.mawa.bes.entity.v2.CashupEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashupRepository extends JpaRepository<CashupEntity, String> {

    Optional<CashupEntity> findByCashupNo(Long cashupNo);

    boolean existsByCashupNo(Long cashupNo);

    List<CashupEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    Optional<CashupEntity> findFirstByDeviceIdAndUserIdAndStatusOrderByCreatedAtDesc(
            String deviceId,
            String userId,
            String status
    );

    Optional<CashupEntity> findFirstByDeviceIdAndUserIdAndStatusAndSourceOrderByCreatedAtDesc(
            String deviceId, String userId, String status, String source
    );

    Optional<CashupEntity> findFirstByLegacyTransactionIdAndSourceOrderByCreatedAtDesc(
            String legacyTransactionId, String source
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CashupEntity> findBySourceAndReceiptBookNoIgnoreCaseOrderByCreatedAtAsc(
            String source,
            String receiptBookNo
    );

    Slice<CashupEntity> findAllByOrderByCashupDateDescCreatedAtDesc(Pageable pageable);

    Slice<CashupEntity> findByStatusIgnoreCaseOrderByCashupDateDescCreatedAtDesc(String status, Pageable pageable);

    List<CashupEntity> findByStatusIgnoreCaseOrderByCashupDateAscCreatedAtAsc(String status);

    List<CashupEntity> findByStatusIgnoreCaseAndCreatedAtLessThanEqualOrderByCashupDateAscCreatedAtAsc(
            String status, java.time.LocalDateTime createdAt);

    @Query(value = """
            SELECT c.*
              FROM cashup c
              LEFT JOIN `user` u ON u.id = c.user_id
              LEFT JOIN partner p ON p.id = u.partner
             WHERE (:status IS NULL OR :status = '' OR UPPER(:status) = 'ALL' OR UPPER(c.status) = UPPER(:status))
               AND (:search IS NULL OR :search = '' OR
                    CAST(c.cashup_no AS CHAR) LIKE CONCAT('%', :search, '%') OR
                    UPPER(COALESCE(c.device_id, '')) LIKE CONCAT('%', UPPER(:search), '%') OR
                    UPPER(COALESCE(c.receipt_book_no, '')) LIKE CONCAT('%', UPPER(:search), '%') OR
                    UPPER(COALESCE(c.employee_responsible_name, '')) LIKE CONCAT('%', UPPER(:search), '%') OR
                    UPPER(COALESCE(c.area_name, '')) LIKE CONCAT('%', UPPER(:search), '%') OR
                    UPPER(COALESCE(u.username, '')) LIKE CONCAT('%', UPPER(:search), '%') OR
                    UPPER(CONCAT_WS(' ', p.name2, p.name3, p.name1)) LIKE CONCAT('%', UPPER(:search), '%'))
             ORDER BY c.cashup_date DESC, c.created_at DESC
            """, nativeQuery = true)
    Slice<CashupEntity> search(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);


    List<CashupEntity> findByUserIdAndCashupDateBetweenOrderByCashupDateDesc(
            String userId,
            LocalDate fromDate,
            LocalDate toDate
    );
}
