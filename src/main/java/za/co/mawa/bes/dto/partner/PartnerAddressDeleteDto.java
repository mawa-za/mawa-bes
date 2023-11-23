package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerAddressDeleteDto {
    private String addressId;
    private String partner;
    private String type;
}
