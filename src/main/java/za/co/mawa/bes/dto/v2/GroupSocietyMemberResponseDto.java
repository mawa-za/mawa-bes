package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSocietyMemberResponseDto {

    private String id;
    private String groupSocietyId;
    private String memberId;
    private String membershipId;
    private String employeeNo;
    private String externalRef;
    private LocalDate joinDate;
    private LocalDate exitDate;
    private String status;
    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
}
