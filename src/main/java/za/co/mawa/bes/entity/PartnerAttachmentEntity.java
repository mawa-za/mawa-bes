package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_attachment")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class PartnerAttachmentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    PartnerAttachmentPKEntity partnerAttachmentPKEntity;
    @Column(name = "file_id")
    private String fileId;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "status")
    private String status;

}
