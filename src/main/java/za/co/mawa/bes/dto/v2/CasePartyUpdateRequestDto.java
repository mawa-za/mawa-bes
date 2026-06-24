package za.co.mawa.bes.dto.v2;

import za.co.mawa.bes.enums.CasePartyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePartyUpdateRequestDto {

    private String id;
    private String caseId;
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
