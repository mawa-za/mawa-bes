package za.co.mawa.bes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDto {

    private String id;
    private String invoiceNo;
    private String externalRef;
    private String partnerId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String status;
    private Long subtotalCents;
    private Long taxCents;
    private Long discountCents;
    private Long totalCents;
    private Long paidCents;
    private Long balanceCents;
    private String currency;
    private String notes;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private List<String> lines;
    private List<String> paymentsIds;
}
