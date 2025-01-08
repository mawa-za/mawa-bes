package za.co.mawa.bes.dto.participant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerBasicDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.PartnerEntity;

@NoArgsConstructor
@Getter
@Setter
public class ParticipantOutboundDto {
    private PartnerDto participant;
    private PartnerBasicDto legalRepresentative;
}
