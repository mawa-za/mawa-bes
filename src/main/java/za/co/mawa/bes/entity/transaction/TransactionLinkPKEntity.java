package za.co.mawa.bes.entity.transaction;

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
@Builder
public class TransactionLinkPKEntity implements Serializable {

    @Column(name = "transaction1")
    private String transaction1;
    @Column(name = "transaction2")
    private String transaction2;
    @Column(name = "type")
    private String type;
}
