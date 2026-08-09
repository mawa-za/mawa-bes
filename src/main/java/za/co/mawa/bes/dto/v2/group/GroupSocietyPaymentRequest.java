package za.co.mawa.bes.dto.v2.group;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GroupSocietyPaymentRequest {
    private Long amountCents;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String referenceId;
    private String referenceNo;
    private String notes;
    private String createdBy;
    private String deviceId;
    private String terminalId;
    private String location;
    private String employeeResponsible;
}
