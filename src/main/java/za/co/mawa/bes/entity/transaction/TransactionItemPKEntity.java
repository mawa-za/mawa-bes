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
public class TransactionItemPKEntity implements Serializable {
    @Column(name = "transaction")
    private String transaction;
    @Column(name = "item")
    private String item;
}
