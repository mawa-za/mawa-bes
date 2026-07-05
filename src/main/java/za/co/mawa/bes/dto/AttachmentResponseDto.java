package za.co.mawa.bes.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDto {

    private String id;
    private String objectId;
    private String documentType;
    private String uploadBy;
    private Date uploadTime;
    private Date uploadDate;
    private String downloadBy;
    private Date downloadDate;
    private String extension;
    private String filePath;
    private String storageBucket;
    private String storageProvider;
    private String contentType;
    private Long fileSize;
}
