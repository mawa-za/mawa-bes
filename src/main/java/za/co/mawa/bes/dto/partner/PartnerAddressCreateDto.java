package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.AddressCreateDto;
import za.co.mawa.bes.dto.FieldOptionDto;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerAddressCreateDto {
    private String id;
    private String partner;
    private String type;
    private Date validFrom;
    private Date validTo;
}
