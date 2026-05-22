package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.enums.ReceiptAllocationType;

import java.util.List;
import java.util.Optional;

public interface ReceiptAllocationRepository extends JpaRepository<ReceiptAllocationEntity, String> {

    List<ReceiptAllocationEntity> findByReceiptId(String receiptId);

    List<ReceiptAllocationEntity> findByMembershipIdOrderByPeriodYYYYMMAsc(String membershipId);

    Optional<ReceiptAllocationEntity> findByAllocationTypeAndReferenceId(
            ReceiptAllocationType allocationType,
            String referenceId
    );

    List<ReceiptAllocationEntity> findByAllocationTypeAndReferenceIdOrderByCreatedAtAsc(
            ReceiptAllocationType allocationType,
            String referenceId
    );
}
