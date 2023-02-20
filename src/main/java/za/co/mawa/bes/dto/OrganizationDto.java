package za.co.mawa.bes.dto;

public class OrganizationDto {
    private String registrationNumber;
    private String VATNo;
    private String name1;
    private String name2;
    private String name3;
    private String telephoneNumber;
    private String emailAddress;
    private AddressDto businessAddress;
    private AddressDto postalAddress;

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVATNo() {
        return VATNo;
    }

    public void setVATNo(String VATNo) {
        this.VATNo = VATNo;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public String getName3() {
        return name3;
    }

    public void setName3(String name3) {
        this.name3 = name3;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public AddressDto getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(AddressDto postalAddress) {
        this.postalAddress = postalAddress;
    }

    public AddressDto getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(AddressDto businessAddress) {
        this.businessAddress = businessAddress;
    }
}
