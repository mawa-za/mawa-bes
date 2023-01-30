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
public class TransactionItemPKEntity implements Serializable {
    @Column(name = "transaction", length = 20)
    private String transaction;
    @Column(name = "item", length = 40)
    private String item;
}
