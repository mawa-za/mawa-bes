package za.co.mawa.bes.dto.transaction;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.claim.ClaimCreateDto;
import za.co.mawa.bes.dto.inquiry.InquiryCreateDto;
import za.co.mawa.bes.dto.invoice.InvoiceCreateDto;
import za.co.mawa.bes.dto.purchase.order.PurchaseOrderCreateDto;
import za.co.mawa.bes.dto.quotation.QuotationCreateDto;
import za.co.mawa.bes.dto.sales.order.SalesOrderCreateDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestCreateDto;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionEditDto implements Serializable {
    private String id;
    private String customerId;
    private String supplierId;
    private String employeeResponsible;
    private String type;
    private String description;
    private String subType;
    private String category;
    private String priority;
    private String status;
    private String statusReason;
    private String changedBy;
    public TransactionEditDto(QuotationCreateDto quotationCreateDto){    }
    public TransactionEditDto(ServiceRequestCreateDto serviceRequestCreateDto){}
    public TransactionEditDto(SalesOrderCreateDto quotationCreateDto){}
    public TransactionEditDto(PurchaseOrderCreateDto purchaseOrderCreateDto){}
    public TransactionEditDto(InvoiceCreateDto invoiceCreateDto){}
    public TransactionEditDto(ClaimCreateDto claimCreateDto){}
    public TransactionEditDto(InquiryCreateDto inquiryCreateDto){}


}