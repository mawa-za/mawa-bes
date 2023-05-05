package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.prospect.ProspectCreateDto;

import java.util.ArrayList;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SupplierDto {


    private String username;
    private String partnerId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String supplierType;

    private String organizationName;
    ArrayList<AddressDto> addressDto;
    ArrayList<ContactDto> contactDto;
    ArrayList<IdentityDto> identityDto;

}
