package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_identity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerIdentityEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerIdentityPKEntity partnerIdentityPK;
    @Column(name = "partner")
    private String partner;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
}
