package za.co.mawa.bes.dto.v2.group;

import java.time.LocalDate;

public class GroupSocietyMemberRequest {

    private String memberId;
    private String membershipId;
    private String employeeNo;
    private String externalRef;
    private LocalDate joinDate;
    private String status;

    public String getMemberId() {
        return memberId;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public String getStatus() {
        return status;
    }
}