package za.co.mawa.bes.dto.transaction.text;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionDateEntity;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class TransactionTextDto implements Serializable {
    private String transaction;
    private String type;
    private String text;

}
