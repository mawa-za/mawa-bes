package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Timestamp;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "attachment")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class AttachmentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "object_id")
    private String objectId;
    @Column(name = "document_type")
    private String documentType;
    @Column(name = "upload_by")
    private String uploadBy;
    @Column(name = "upload_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadTime;

    @Column(name = "upload_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadDate;

    @Column(name = "download_by")
    private String downloadBy;

    @Column(name = "download_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date downloadDate;
    /**
     * Legacy column. New uploads are stored in Google Cloud Storage and this column is kept null.
     * Existing records can be migrated with POST /v2/attachment/migrate-to-gcp.
     */
    @Lob
    @Column(name = "file")
    private byte[] file;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "storage_bucket")
    private String storageBucket;

    @Column(name = "storage_provider")
    private String storageProvider;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "extension")
    private String extension;
}
