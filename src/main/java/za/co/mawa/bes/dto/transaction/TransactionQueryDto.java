package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.claim.ClaimQueryDto;
import za.co.mawa.bes.dto.inquiry.InquiryQueryDto;
import za.co.mawa.bes.dto.invoice.InvoiceQueryDto;
import za.co.mawa.bes.dto.purchase.order.PurchaseOrderQueryDto;
import za.co.mawa.bes.dto.quotation.QuotationQueryDto;
import za.co.mawa.bes.dto.sales.order.SalesOrderQueryDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestQueryDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionQueryDto implements Serializable {
    private String type;
    private String subtype;
    private String status;
    private String partnerFunction;
    private String partnerNo;
    private String number;
    private Date value;
    private String dateType;
    private String transactionlink1;
    private String createdBy;
    private String changedBy;


    public TransactionQueryDto(QuotationQueryDto quotationQueryDto) {
    }

    public TransactionQueryDto(ServiceRequestQueryDto serviceRequestQueryDto) {
    }

    public TransactionQueryDto(SalesOrderQueryDto quotationQueryDto) {
    }

    public TransactionQueryDto(PurchaseOrderQueryDto purchaseOrderQueryDto) {
    }

    public TransactionQueryDto(InvoiceQueryDto invoiceQueryDto) {
    }

    public TransactionQueryDto(ClaimQueryDto claimQueryDto) {
    }

    public TransactionQueryDto(InquiryQueryDto inquiryQueryDto) {
    }

}
