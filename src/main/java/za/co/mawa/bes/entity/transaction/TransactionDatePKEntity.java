package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;


import java.io.Serializable;
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionDatePKEntity implements Serializable {
    @Column(name = "transaction")
    private String transaction;
    @Column(name = "type")
    private String type;


}
