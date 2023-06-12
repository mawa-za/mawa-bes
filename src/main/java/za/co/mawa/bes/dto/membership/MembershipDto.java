package za.co.mawa.bes.dto.membership;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MembershipDto {
    private String memberId;
    private String member;
    private String salesRepresentativeId;
    private String salesRepresentative;
    private String productId;
    private String productDescription;
    private BigDecimal premium;
    private Date dateJoined;
    private Date dateEffective;
    private String status;
    private String statusReason;

    private PersonDto mainMember;
    private PersonDto salesRep;
    List<DependentDto> dependentDtoList;
}
