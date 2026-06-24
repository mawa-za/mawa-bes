package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerRelationPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner1", length = 20)
    private String partner1;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner2", length = 20)
    private String partner2;
}
