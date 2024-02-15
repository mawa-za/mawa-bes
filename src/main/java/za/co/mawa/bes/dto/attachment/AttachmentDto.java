package za.co.mawa.bes.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AttachmentDto implements Serializable {
   private String id;
   private FieldOptionDto documentType;
   private String extension;
   private String objectId;
   private Date uploadDate;
   private Date uploadTime;
   private PartnerDto uploadBy;
   private PartnerDto createdBy;
}
