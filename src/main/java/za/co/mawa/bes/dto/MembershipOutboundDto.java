package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MembershipOutboundDto implements Serializable {
    private String memberId;
    private String membershipNo;
    private String fullName;
    private String idNumber;
    private String planId;
    private String paidUpToPeriod;
    private boolean active;
    private String updatedAt;
}
