package za.co.mawa.bes.dto.group.society;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GroupSocietyCreateDto implements Serializable {
    private String memberId;
    private String salesRepresentativeId;
    private String membershipType;
    private String productId;
    private String creationType;
    private String salesArea;
    private Date dateJoined;
    private String previousInsurerId;
    private Date lastReceiptDate;
}
