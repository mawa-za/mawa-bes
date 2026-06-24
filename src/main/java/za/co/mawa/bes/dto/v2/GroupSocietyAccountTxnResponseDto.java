package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSocietyAccountTxnResponseDto {

    private String id;
    private String groupSocietyId;
    private String txnType;
    private String direction;
    private Long amountCents;
    private Long balanceBeforeCents;
    private Long balanceAfterCents;
    private LocalDate txnDate;
    private Date txnDatetime;
    private String referenceType;
    private String referenceId;
    private String referenceNo;
    private String paymentMethod;
    private String period;
    private String notes;
    private Date createdAt;
    private String createdBy;
}
