package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaveProfileRuleDto {
    private String id;
    private String leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private BigDecimal entitlementAmount;
    private Integer cycleMonths;
    private String accrualMethod;
    private String accrualFrequency;
    private BigDecimal accrualAmount;
    private Boolean proRata;
    private Boolean carryOverAllowed;
    private BigDecimal maximumCarryOver;
    private Integer carryOverExpiryMonths;
    private BigDecimal maximumNegativeBalance;
    private Integer waitingPeriodDays;
    private Boolean supportingDocumentRequiredOverride;
    private LocalDate activeFrom;
    private LocalDate activeTo;
    private Boolean active;
    private Long version;
}
