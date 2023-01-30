package za.co.mawa.bes.dto;

import java.io.Serializable;

public class PartnerDateDto implements Serializable {
    private String partnerNo;
    private String type;
    private String typeDescription;
    private String value;

    public String getPartnerNo() {
        return partnerNo;
    }

    public void setPartnerNo(String partnerNo) {
        this.partnerNo = partnerNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
