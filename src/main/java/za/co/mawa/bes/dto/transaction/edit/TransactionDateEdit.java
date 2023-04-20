package za.co.mawa.bes.dto.transaction.edit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class TransactionDateEdit implements Serializable {
    private String transaction;
    private String type;
    private Date value;
}
