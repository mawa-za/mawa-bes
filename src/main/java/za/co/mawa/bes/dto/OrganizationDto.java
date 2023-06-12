package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class OrganizationDto {
    private String regNumber;
    private String VATNo;
    private String name1;
    private String name2;
    private String name3;
    private String telephoneNumber;
    private String emailAddress;
    private AddressDto businessAddress;
    private AddressDto postalAddress;


}
