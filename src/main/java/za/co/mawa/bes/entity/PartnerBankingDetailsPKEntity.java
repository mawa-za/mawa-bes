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
public class PartnerBankingDetailsPKEntity implements Serializable {
    @Basic(optional = false)
    @Column(name = "partner")
    private String partner;
    @Basic(optional = false)
    @Column(name = "type")
    private String type;

    @Column(name = "account_number")
    private String accountNumber;
}
