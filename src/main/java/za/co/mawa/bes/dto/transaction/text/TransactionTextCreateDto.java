package za.co.mawa.bes.dto.transaction.text;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionTextCreateDto implements Serializable {
    private String transaction;
    private String type;
    private String text;
}
