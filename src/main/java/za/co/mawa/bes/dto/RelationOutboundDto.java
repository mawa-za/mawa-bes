package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;

@NoArgsConstructor
@Getter
@Setter

public class RelationOutboundDto {

    private PartnerDto partner1;
    private PartnerDto partner2;
    private FieldOptionDto type;
}
