package za.co.mawa.bes.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CommentDto {
    private String id;
    private String parentTransactionId;
    private String description;
    private String type;
    private String subType;
    private PartnerDto createdBy;
    private PartnerDto changedBy;
    private Date createdDate;
    private Date lastUpdated;
}
