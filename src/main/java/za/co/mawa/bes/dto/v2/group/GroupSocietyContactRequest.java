package za.co.mawa.bes.dto.v2.group;

public class GroupSocietyContactRequest {

    private String contactName;
    private String role;
    private String mobileNo;
    private String email;
    private Boolean primaryContact;

    public String getContactName() {
        return contactName;
    }

    public String getRole() {
        return role;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getPrimaryContact() {
        return primaryContact;
    }
}