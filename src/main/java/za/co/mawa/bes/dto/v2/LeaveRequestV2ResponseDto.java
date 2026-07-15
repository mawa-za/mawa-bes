package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class LeaveRequestV2ResponseDto {
    private String id;
    private String requestNumber;
    private FieldOptionDto type;
    private PartnerDto employee;
    private PartnerDto approver;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private FieldOptionDto status;
    private String statusReason;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LeaveRequestStatusHistoryV2Dto> statusHistory;
}
