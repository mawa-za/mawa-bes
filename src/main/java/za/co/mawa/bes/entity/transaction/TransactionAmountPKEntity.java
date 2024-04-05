package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@EqualsAndHashCode
public class TransactionAmountPKEntity implements Serializable {
    @Column(name = "transaction")
    private String transaction;
    @Column(name = "type")
    private String type;

}
