package za.co.mawa.bes.dto.participant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

@NoArgsConstructor
@Getter
@Setter
public class ParticipantDto {
    private FieldOptionDto function;
    private PartnerDto partner;
}
