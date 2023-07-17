package za.co.mawa.bes.dto.voucher;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class VoucherCreateDto implements Serializable {
    private BigDecimal amount;
    private String customerId;
    private String expiryDate;
    private String type;
}
