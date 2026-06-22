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
public class PartnerDatePKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner_no", length = 20)
    private String partner_no;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;
}
