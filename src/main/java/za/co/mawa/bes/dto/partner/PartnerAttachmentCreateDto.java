package za.co.mawa.bes.dto.partner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PartnerAttachmentCreateDto implements Serializable {
    private String partner;
    private String documentType;
    private String attachmentId;
}
