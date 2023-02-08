package za.co.mawa.bes.entity;

import jakarta.persistence.*;
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
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "file_type")
    private String fileType;
    @Column(name = "file_content")
    private byte[] fileContent;
    @Column(name = "creation_by")
    private Date creationBy;
    @Column(name = "creation_date")
    private Date creationDate;


}
