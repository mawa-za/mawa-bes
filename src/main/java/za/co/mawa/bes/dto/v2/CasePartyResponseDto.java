package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.CasePartyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePartyResponseDto {

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
    private LocalDateTime createdAt;
    private String createdBy;
}
