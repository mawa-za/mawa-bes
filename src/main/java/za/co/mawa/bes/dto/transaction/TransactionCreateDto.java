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
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class TransactionCreateDto implements Serializable {
    private String type;
    private String parentId;
    private String customerId;
    private String supplierId;
    private String employeeResponsible;
    private String location;
    private String description;
    private String subType;
    private String category;
    private String priority;
    private String status;
    private String statusReason;
    private String plannedStartDate;
    private String plannedEndDate;
    private String subDescription;
    private String createdBy;
    private Date endDate;
    private Date startDate;
    private String summary;

    public TransactionCreateDto(QuotationCreateDto quotationCreateDto){
    }
    public TransactionCreateDto(ServiceRequestCreateDto serviceRequestCreateDto){}
    public TransactionCreateDto(SalesOrderCreateDto quotationCreateDto){}
    public TransactionCreateDto(PurchaseOrderCreateDto purchaseOrderCreateDto){}
    public TransactionCreateDto(InvoiceCreateDto invoiceCreateDto){}
    public TransactionCreateDto(ClaimCreateDto claimCreateDto){}
    public TransactionCreateDto(InquiryCreateDto inquiryCreateDto){}


}
