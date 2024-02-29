package za.co.mawa.bes.dto.group.society;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.AmountDto;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductBasicDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.utils.TransactionAmount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GroupSocietyDto {
    private String id;
    private FieldOptionDto type;
    private String number;
    private PartnerDto customer;
    private PartnerDto salesRepresentative;
    private ProductBasicDto product;
    private Date dateCreated;
    private Date dateJoined;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private FieldOptionDto salesArea;
    private List<AmountDto> amounts = new ArrayList<>();
}
