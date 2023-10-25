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
public class AttachmentDto implements Serializable {
   private String id;
   private String uploadDate;
   private String uploadTime;
   private String uploadedBy;
   private String downloadDate;
   private String downloadedBy;
   private String file;
}
