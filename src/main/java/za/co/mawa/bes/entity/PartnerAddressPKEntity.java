package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
public class PartnerAddressPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner")
    private String partner;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "address_id")
    private String addressId;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "address_usage", length = 20)
    private String addressUsage;

}
