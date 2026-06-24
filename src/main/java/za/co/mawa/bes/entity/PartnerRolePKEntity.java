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
public class PartnerRolePKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner")
    private String id;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "role", length = 20)
    private String role;

}
