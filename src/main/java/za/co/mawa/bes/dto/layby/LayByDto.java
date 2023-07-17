package za.co.mawa.bes.dto.layby;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LayByDto implements Serializable {
    private String id;
    private String number;
    private String status;
    private String statusReason;
    private BigDecimal amount;
    private ProductDto productDetails;
    private String createdBy;
    private String dateCreated;
    private String endDate;
    private String period;
    private ArrayList<ReceiptDto> receipts;
    private PartnerDto customer;
    private PartnerDto salesRepresentative;
}
