package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaveTypeDto {
    private String id;
    private String code;
    private String name;
    private String description;
    private Boolean paid;
    private String unit;
    private Boolean allowHalfDay;
    private Boolean requiresSupportingDocument;
    private BigDecimal documentRequiredAfter;
    private BigDecimal minimumRequest;
    private BigDecimal maximumConsecutive;
    private Boolean allowNegativeBalance;
    private Boolean includeWeekends;
    private Boolean includePublicHolidays;
    private Boolean requiresApproval;
    private LocalDate activeFrom;
    private LocalDate activeTo;
    private Integer displayOrder;
    private String colour;
    private String icon;
    private Boolean active;
    private Long version;
}
