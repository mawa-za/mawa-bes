package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseNoteType;

@Getter
@Setter
public class CaseNoteCreateRequest {
    private CaseNoteType noteType;
    private String title;
    private String note;
    private Boolean privateNote;
}
