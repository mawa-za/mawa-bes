package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PartnerAttachmentDto implements Serializable {
    private String partner;
    private String fileId;
    private String documentType;
    private String validFrom;
    private String validTo;
    private String status;

}
