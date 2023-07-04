package za.co.mawa.bes.dto.deposit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DepositCreateDto implements Serializable {
    private String amount;
    private String transactionIdLink;
}
