package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipClaimLinkResponseDto {

    private String id;
    private String parentClaimId;
    private String linkedClaimId;
    private LocalDateTime createdAt;
    private String createdBy;
}
