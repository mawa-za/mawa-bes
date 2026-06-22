package za.co.mawa.bes.entity.transaction;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class TransactionPartnerPKEntity implements Serializable {
    @Column(name = "transaction")
    private String transaction;
    @Column(name = "partner_function")
    private String function;
    @Column(name = "partner")
    private String partner;

}
