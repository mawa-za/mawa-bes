package za.co.mawa.bes.service.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.v2.ReceiptAllocationResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;

import java.util.List;

@Component("serviceReceiptMapper")
public class ReceiptMapper {

    public ReceiptResponseDto toDto(
            ReceiptEntity receipt,
            List<ReceiptAllocationEntity> allocations
    ) {
        return ReceiptResponseDto.builder()
                .id(receipt.getId())
                .receiptNo(receipt.getReceiptNo())
                .traceId(receipt.getTraceId())
                .paymentBatchId(receipt.getPaymentBatchId())
                .paymentBatchNo(receipt.getPaymentBatchNo())
                .sourceType(receipt.getSourceType())
                .membershipId(receipt.getMembershipId())
                .receiptDate(receipt.getReceiptDate())
                .paymentMethod(receipt.getPaymentMethod())
                .totalAmountCents(receipt.getTotalAmountCents())
                .status(receipt.getStatus())
                .syncStatus(receipt.getSyncStatus())
                .printed(receipt.getPrinted())
                .printCount(receipt.getPrintCount())
                .captureSource(receipt.getCaptureSource())
                .manualReceiptBookNo(receipt.getManualReceiptBookNo())
                .manualReceiptNo(receipt.getManualReceiptNo())
                .allocations(allocations.stream().map(this::toAllocationDto).toList())
                .build();
    }

    public ReceiptAllocationResponseDto toAllocationDto(ReceiptAllocationEntity allocation) {
        return ReceiptAllocationResponseDto.builder()
                .id(allocation.getId())
                .allocationType(allocation.getAllocationType())
                .referenceId(allocation.getReferenceId())
                .referenceNo(allocation.getReferenceNo())
                .periodYYYYMM(allocation.getPeriodYYYYMM())
                .membershipId(allocation.getMembershipId())
                .amountCents(allocation.getAmountCents())
                .status(allocation.getStatus())
                .build();
    }
}