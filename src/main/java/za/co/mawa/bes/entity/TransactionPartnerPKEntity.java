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
    @Column(name = "transaction")
    private String transaction;
    @Column(name = "function")
    private String function;
    @Column(name = "partner")
    private String partner;


}
