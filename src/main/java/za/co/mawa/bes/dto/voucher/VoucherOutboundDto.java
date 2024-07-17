package za.co.mawa.bes.dto.voucher;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class VoucherOutboundDto implements Serializable {
    private String id;
    private String number;
    private FieldOptionDto type;
    private PartnerDto recipient;
    private String dateCreated;
    private String expiryDate;
    private String status;
    private String statusReason;
    private BigDecimal amount;
    private String createdBy;
    private String changedBy;
    private PartnerDto customer;
}
