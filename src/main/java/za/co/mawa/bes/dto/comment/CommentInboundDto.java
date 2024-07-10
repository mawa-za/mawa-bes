package za.co.mawa.bes.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CommentInboundDto {

    private String parentTransactionId;
    private String description;

}
