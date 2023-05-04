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



    private String username;
    private String partnerId;

    AddressDto addressDto;
    ContactDto contactDto;
    IdentityDto identityDto;

}
