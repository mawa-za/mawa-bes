package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder
public class LeaveLedgerDto {
    private String id;
    private String employmentId;
    private String leaveTypeId;
    private String leaveTypeCode;
    private String transactionType;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceType;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
}
