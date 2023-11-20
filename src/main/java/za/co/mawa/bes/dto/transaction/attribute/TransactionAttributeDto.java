package za.co.mawa.bes.dto.transaction.attribute;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionAttributeDto implements Serializable {
    private String transaction;
    private String attribute;
    private String value;

}
