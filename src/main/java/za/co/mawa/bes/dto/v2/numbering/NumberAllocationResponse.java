package za.co.mawa.bes.dto.v2.numbering;

public class NumberAllocationResponse {

    private String seqType;
    private String deviceId;

    private Long fromNo;
    private Long toNo;
    private Long nextLocalNo;

    private Integer allocationSize;
    private String status;

    public NumberAllocationResponse() {
    }

    public NumberAllocationResponse(
            String seqType,
            String deviceId,
            Long fromNo,
            Long toNo,
            Long nextLocalNo,
            Integer allocationSize,
            String status
    ) {
        this.seqType = seqType;
        this.deviceId = deviceId;
        this.fromNo = fromNo;
        this.toNo = toNo;
        this.nextLocalNo = nextLocalNo;
        this.allocationSize = allocationSize;
        this.status = status;
    }

    public String getSeqType() {
        return seqType;
    }

    public void setSeqType(String seqType) {
        this.seqType = seqType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getFromNo() {
        return fromNo;
    }

    public void setFromNo(Long fromNo) {
        this.fromNo = fromNo;
    }

    public Long getToNo() {
        return toNo;
    }

    public void setToNo(Long toNo) {
        this.toNo = toNo;
    }

    public Long getNextLocalNo() {
        return nextLocalNo;
    }

    public void setNextLocalNo(Long nextLocalNo) {
        this.nextLocalNo = nextLocalNo;
    }

    public Integer getAllocationSize() {
        return allocationSize;
    }

    public void setAllocationSize(Integer allocationSize) {
        this.allocationSize = allocationSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}