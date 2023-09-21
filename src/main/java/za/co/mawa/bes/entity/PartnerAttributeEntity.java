package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;
@Entity
@Table(name = "partner_attribute")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class PartnerAttributeEntity implements Serializable {
    @EmbeddedId
    PartnerAttributePKEntity partnerAttributePKEntity;
    @Column(name = "value")
    private String value;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

}
