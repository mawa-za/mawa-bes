package za.co.mawa.bes.dto.invoice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.PricingOutboundDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.user.UserDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class InvoiceOutboundDto implements Serializable {
    private String id;
    private FieldOptionDto type;
    private String number;
    private PartnerDto customer;
    private PartnerDto salesRepresentative;
    private Date dueDate;
    private Date invoiceDate;
    private FieldOptionDto paymentTerms;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private List<LineItemOutboundDto> items;
    private List<TransactionAmountOutboundDto> amounts;
    private List<TransactionDateDto> dates;
    private String transactionId;
    private FieldOptionDto invoiceType;
    private PartnerDto createdBy;

    private TransactionDto transactionSubType;
}

