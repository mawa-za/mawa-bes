package za.co.mawa.bes.dto.cashup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.receipt.ReceiptDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CashupEditDto implements Serializable{
    private String amountDeposited;
    private String status;
}
