package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerContactPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner")
    private String partner;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;

}
