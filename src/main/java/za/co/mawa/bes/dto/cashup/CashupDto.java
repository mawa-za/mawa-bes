package za.co.mawa.bes.dto.cashup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CashupDto implements Serializable{

    private String id;
    private String number;
    private FieldOptionDto status;
    private PartnerDto createdBy;
    private PartnerDto changedBy;
    private ArrayList<ReceiptDto> receipts;
    private BigDecimal amountCollected;
   // private String amountCollected;
    private BigDecimal amountDeposited;
    private Date createdDate;
    private Date lastUpdated;
    private FieldOptionDto salesArea;
    private PartnerDto employeeResponsible;
    private String cashUpType;
    private List<TransactionAttributeEntity> attributes;

}
