package za.co.mawa.bes.dto.v2.group;

public class GroupSocietyRequest {

    private String partnerId;
    private String groupNo;
    private String societyType;
    private String status;
    private Long openingBalanceCents;

    public String getPartnerId() {
        return partnerId;
    }

    public String getGroupNo() {
        return groupNo;
    }

    public String getSocietyType() {
        return societyType;
    }

    public String getStatus() {
        return status;
    }

    public Long getOpeningBalanceCents() {
        return openingBalanceCents;
    }
}