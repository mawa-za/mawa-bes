package za.co.mawa.bes.dto.transaction.edit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionEdit implements Serializable {
    private String id;
   // private String type;
   // private String subtype;
    private String status;
    private String statusReason;
    private String description;
}
