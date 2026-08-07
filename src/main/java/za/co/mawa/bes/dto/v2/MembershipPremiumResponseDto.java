package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.PremiumStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPremiumResponseDto {

    private String id;
    private String membershipId;
    private String periodYYYYMM;
    private Long amountCents;
    private Long paidAmountCents;
    private Long balanceCents;
    private PremiumStatus status;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Latest posted receipt context for a more useful premium history view.
    private String receiptId;
    private String receiptNo;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String cashier;
    private String paymentLocation;
    private String deviceId;
    private Integer paymentCount;
}
