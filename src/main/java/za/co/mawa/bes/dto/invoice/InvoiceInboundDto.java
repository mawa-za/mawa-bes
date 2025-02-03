package za.co.mawa.bes.dto.invoice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.PricingInboundDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class InvoiceInboundDto implements Serializable {
    private String customerId;
    private String salesRepresentative;
    private Date dueDate;
    private Date invoiceDate;
    private String paymentTerms;
    private PricingInboundDto pricing;
    private List<LineItemInboundDto> items;
    private String subTransactionId;
    private String invoiceType;
    private String transactionSubType;

}