package za.co.mawa.bes.entity;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionPartnerPKEntity implements Serializable {
    @Column(name = "transaction", length = 20)
    private String transaction;
    @Column(name = "partner_function", length = 20)
    private String partnerFunction;
    @Column(name = "partner_no", length = 20)
    private String partnerNo;


}
