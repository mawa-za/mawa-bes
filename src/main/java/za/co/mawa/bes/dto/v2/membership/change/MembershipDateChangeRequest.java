package za.co.mawa.bes.dto.v2.membership.change;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MembershipDateChangeRequest {
    private LocalDate startDate;
    private LocalDate effectiveDate;
    private String reason;
}
