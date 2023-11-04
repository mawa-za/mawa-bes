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
public class CashupDto implements Serializable{

    private String id;
    private String number;
    private String status;
    private String createdBy;
    private String changedBy;
    private ArrayList<ReceiptDto> receipts;
    private String amountCollected;
   // private String amountCollected;
    private String amountDeposited;
    private Date createdDate;
    private Date lastUpdated;
    private String salesArea;
    private String employeeResponsible;
}
