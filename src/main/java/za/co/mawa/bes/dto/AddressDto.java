package za.co.mawa.bes.dto;

import java.io.Serializable;

public class AddressDto implements Serializable {
    private int id;
    private String partner;
    private String type;
    private String line1;
    private String line2;
    private String line3;
    private String line4;
    private String postalCode;
    private String typeDescription;
    private String line3Description;
    private String line4Description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getLine3() {
        return line3;
    }

    public void setLine3(String line3) {
        this.line3 = line3;
    }

    public String getLine4() {
        return line4;
    }

    public void setLine4(String line4) {
        this.line4 = line4;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getLine3Description() {
        return line3Description;
    }

    public void setLine3Description(String line3Description) {
        this.line3Description = line3Description;
    }

    public String getLine4Description() {
        return line4Description;
    }

    public void setLine4Description(String line4Description) {
        this.line4Description = line4Description;
    }
}
