package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerCreateDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityCreateDto;

import java.io.Serializable;
import java.util.ArrayList;

@NoArgsConstructor
@Getter
@Setter
public class TenantCreateDto implements Serializable {
    private String tenantName;
  //  private ContactCreateDto tenantContact;
 //   private AddressCreateDto tenantAddress;
//    private PartnerIdentityCreateDto tenantIdentity;
//    private ArrayList<TenantPropertyDto> tenantAttributes;

}
