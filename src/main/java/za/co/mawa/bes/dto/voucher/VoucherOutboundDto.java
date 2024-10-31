package za.co.mawa.bes.dto.voucher;

import jakarta.mail.Part;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.user.UserDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class VoucherOutboundDto implements Serializable {
    private String id;
    private String number;
    private FieldOptionDto type;
    private PartnerDto recipient;
    private Date dateCreated;
    private Date expiryDate;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private BigDecimal amount;
    private List<TransactionAmountOutboundDto> amounts;
    private PartnerDto createdBy;
    private PartnerDto changedBy;
    private PartnerDto customer;
}