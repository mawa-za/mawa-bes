package za.co.mawa.bes.dto.v2;

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
public class FuneralServiceInvoiceResponseDto {

    private String id;
    private String funeralServiceId;
    private String invoiceId;
    private String entityType;
    private String partnerId;
    private String membershipClaimId;
    private Long amountCents;
    private LocalDateTime createdAt;
}
