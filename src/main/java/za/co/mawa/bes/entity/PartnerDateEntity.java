package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_date")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDateEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerDatePKEntity partnerDatePK;
    @Column(name = "value")
    @Temporal(TemporalType.TIMESTAMP)
    private Date value;
}
