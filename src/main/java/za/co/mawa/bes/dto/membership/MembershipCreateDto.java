package za.co.mawa.bes.dto.membership;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MembershipCreateDto implements Serializable {
    private String memberId;
    private String salesRepresentativeId;
    private String membershipType;
    private String productId;
    private String creationType;
    private String salesArea;
    private Date dateJoined;
    private String previousInsurerId;
    private String currentMembershipId;
    private Date lastReceiptDate;

}
