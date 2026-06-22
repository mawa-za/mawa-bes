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
public class PartnerIdentityPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "value", length = 60)
    private String value;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;
}
