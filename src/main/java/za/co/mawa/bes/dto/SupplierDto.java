package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.prospect.ProspectCreateDto;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SupplierDto {

    private String partnerType;
    private String surname;
    private String firstName;
    private String middleName;

    private String username;
    private String organisationName;


    AddressDto addressDto;
    ContactDto contactDto;
    IdentityDto identityDto;

}
