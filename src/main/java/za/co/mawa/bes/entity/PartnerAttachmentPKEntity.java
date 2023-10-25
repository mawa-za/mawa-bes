package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class PartnerAttachmentPKEntity implements Serializable {
    @Column(name = "partner")
    private String partner;
    @Column(name = "document_type")
    private String documentType;
}
