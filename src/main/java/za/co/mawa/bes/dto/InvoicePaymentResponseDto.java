package za.co.mawa.bes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentResponseDto {

    private String id;
    private String invoice;
    private String paymentDateId;
    private String amountCentsId;
    private String paymentMethodId;
    private String referenceNoId;
    private LocalDateTime createdAt;
    private String createdBy;
}
