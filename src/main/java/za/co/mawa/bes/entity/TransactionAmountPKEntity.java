package za.co.mawa.bes.entity;

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
public class TransactionAmountPKEntity implements Serializable {
    @Column(name = "transaction", length = 20)
    private String transaction;

    @Column(name = "type", length = 20)
    private String type;

}
