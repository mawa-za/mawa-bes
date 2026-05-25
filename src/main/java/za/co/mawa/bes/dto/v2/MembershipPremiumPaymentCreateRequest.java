package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MembershipPremiumPaymentCreateRequest {

    private String membershipId;

    private String paymentMethod;

    private Long amountCents;

    private LocalDateTime paymentDate;

    private String location;

    private String employeeResponsible;

    private String deviceId;

    private String terminalId;

    private String createdBy;

    private String notes;
}