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
    @Lob
    @Column(name = "file")
    private byte[] file;

    @Column(name = "extension")
    private String extension;
}
