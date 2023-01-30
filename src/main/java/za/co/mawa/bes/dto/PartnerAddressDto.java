package za.co.mawa.bes.dto;

public class PartnerAddressDto {
    private int id;
    private String patner;
    private String type;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPatner() {
        return patner;
    }

    public void setPatner(String patner) {
        this.patner = patner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
