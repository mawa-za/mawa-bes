package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSocietyContactUpdateRequestDto {

    private String id;
    private String groupSocietyId;
    private String contactName;
    private String role;
    private String mobileNo;
    private String email;
    private Boolean primaryContact;
}
