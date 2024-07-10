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
public class CommentInboundDto {

    private String parentTransactionId;
    private String description;

}
