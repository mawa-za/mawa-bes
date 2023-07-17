package za.co.mawa.bes.dto.voucher;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.PartnerDto;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class VoucherDto implements Serializable {
    private String id;
    private String number;
    private String type;
    private PartnerDto customer;
    private String dateCreated;
    private String expiryDate;
    private String status;
    private String statusReason;
    private BigDecimal amount;
    private String createdBy;
    private String changedBy;
}
