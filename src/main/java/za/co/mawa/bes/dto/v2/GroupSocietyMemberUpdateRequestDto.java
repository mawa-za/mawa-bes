package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSocietyMemberUpdateRequestDto {

    private String id;
    private String groupSocietyId;
    private String memberId;
    private String membershipId;
    private String employeeNo;
    private String externalRef;
    private LocalDate joinDate;
    private LocalDate exitDate;
    private String status;
}
