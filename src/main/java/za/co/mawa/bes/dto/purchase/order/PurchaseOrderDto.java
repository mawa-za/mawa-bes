package za.co.mawa.bes.dto.purchase.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PurchaseOrderDto implements Serializable {
    private String createdBy;
    private String changedBy;
    private String id;
    private String number;
    private String status;
    private String statusReason;
    private BigDecimal amount;
    private PartnerDto supplierDetails;
    private String orderDate;
    private String expectedDate;
    private List<LineItemDto> items;
    private ArrayList<ReceiptDto> receipts;
    private String paymentMethod;

}
