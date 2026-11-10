package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentBatchRepository extends JpaRepository<PaymentBatchEntity, String> {

    Optional<PaymentBatchEntity> findByPaymentBatchNo(String paymentBatchNo);

    Optional<PaymentBatchEntity> findByDeviceIdAndLocalPaymentBatchId(
            String deviceId,
            String localPaymentBatchId
    );

    List<PaymentBatchEntity> findByMembershipIdOrderByPaymentDateDesc(String membershipId);

    @org.springframework.data.jpa.repository.Query("select p from PaymentBatchEntity p where upper(p.paymentMethod) = 'EFT' and "
            + "p.bankDepositId is null and p.status = :posted "
            + "and (:query = '' or lower(p.paymentBatchNo) like lower(concat('%',:query,'%')) "
            + "or lower(coalesce(p.membershipId,'')) like lower(concat('%',:query,'%'))) "
            + "order by p.createdAt desc")
    org.springframework.data.domain.Page<PaymentBatchEntity> pendingBankVerification(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("posted") za.co.mawa.bes.enums.PaymentBatchStatus posted,
            org.springframework.data.domain.Pageable page);

    boolean existsByPaymentBatchNo(String paymentBatchNo);
}