package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.entity.PartnerAttributePKEntity;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_attribute")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionAttributeEntity implements Serializable {
    @EmbeddedId
    TransactionAttributePKEntity transactionAttributePKEntity;
    @Column(name = "value")
    private String value;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

}
