package za.co.mawa.bes.dto.v2.membership.claim;

import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;

public class LinkedMembershipClaimResponse {

    private String linkId;
    private String claimId;
    private String claimNo;
    private String membershipId;

    private MembershipClaimType claimType;

    private Long claimAmountCents;

    private MembershipClaimStatus status;

    public String getLinkId() {
        return linkId;
    }

    public LinkedMembershipClaimResponse setLinkId(String linkId) {
        this.linkId = linkId;
        return this;
    }

    public String getClaimId() {
        return claimId;
    }

    public LinkedMembershipClaimResponse setClaimId(String claimId) {
        this.claimId = claimId;
        return this;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public LinkedMembershipClaimResponse setClaimNo(String claimNo) {
        this.claimNo = claimNo;
        return this;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public LinkedMembershipClaimResponse setMembershipId(String membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public MembershipClaimType getClaimType() {
        return claimType;
    }

    public LinkedMembershipClaimResponse setClaimType(MembershipClaimType claimType) {
        this.claimType = claimType;
        return this;
    }

    public Long getClaimAmountCents() {
        return claimAmountCents;
    }

    public LinkedMembershipClaimResponse setClaimAmountCents(Long claimAmountCents) {
        this.claimAmountCents = claimAmountCents;
        return this;
    }

    public MembershipClaimStatus getStatus() {
        return status;
    }

    public LinkedMembershipClaimResponse setStatus(MembershipClaimStatus status) {
        this.status = status;
        return this;
    }
}