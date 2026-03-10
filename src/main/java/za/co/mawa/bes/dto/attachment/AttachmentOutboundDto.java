package za.co.mawa.bes.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AttachmentOutboundDto implements Serializable {
    private String file;
    private String extension;
    private String documentType;
    private String objectType;
    private String objectId;

}
