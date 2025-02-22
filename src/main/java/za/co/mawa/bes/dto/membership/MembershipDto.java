package za.co.mawa.bes.dto.membership;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.invoice.InvoiceOutboundDto;
import za.co.mawa.bes.dto.partner.PartnerBasicDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductBasicDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkDto;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;


import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MembershipDto {
    private String id;
    private FieldOptionDto type;
    private String number;
    private PartnerDto member;
    private PartnerDto salesRepresentative;
    private ProductBasicDto product;
    private BigDecimal premium;
    private Date dateJoined;
    private Date dateEffective;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private List<TransactionLinkDto> membershipHistoryLinks;
    private List<MembershipDto> membershipHistory;
    private List<InvoiceOutboundDto> invoiceHistory;
    private List<InvoiceOutboundDto> invoices;
}

