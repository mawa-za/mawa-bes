package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CasePartyType;

@Getter
@Setter
public class CasePartyCreateRequest {
    private String partnerId;
    private String partyName;
    private CasePartyType partyType;
    private String idNumber;
    private String email;
    private String phoneNumber;
    private String attorneyFirm;
    private String attorneyName;
    private String notes;
}
