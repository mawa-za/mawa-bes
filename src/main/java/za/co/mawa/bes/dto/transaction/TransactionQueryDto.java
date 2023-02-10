package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.*;

import java.io.Serializable;

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
