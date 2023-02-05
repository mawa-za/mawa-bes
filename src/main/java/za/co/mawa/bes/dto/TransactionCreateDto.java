package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionCreateDto implements Serializable {
    private String number;
    private String customer;
    private String supplier;
    private String claimant;
    private String description;
    private String type;
    private String subType;
    private String status;
    public TransactionCreateDto(QuotationCreateDto quotationCreateDto){
    }
    public TransactionCreateDto(ServiceRequestCreateDto serviceRequestCreateDto){}
    public TransactionCreateDto(SalesOrderCreateDto quotationCreateDto){}
    public TransactionCreateDto(PurchaseOrderCreateDto purchaseOrderCreateDto){}
    public TransactionCreateDto(InvoiceCreateDto invoiceCreateDto){}
    public TransactionCreateDto(ClaimCreateDto claimCreateDto){}
    public TransactionCreateDto(InquiryCreateDto inquiryCreateDto){}


}
