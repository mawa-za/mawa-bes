package za.co.mawa.bes.dto.voucher;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class VoucherEditDto implements Serializable {
    private BigDecimal amount;
    private String status;
    private Date expiryDate;
    private String statusReason;
    private String contractId;
    private String changedBy;
}
