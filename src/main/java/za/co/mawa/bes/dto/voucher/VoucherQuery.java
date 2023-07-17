package za.co.mawa.bes.dto.voucher;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class VoucherQuery implements Serializable {
    private String status;
    private String customerId;
    private String number;
    private String dateCreated;
    private String expiryDate;
    private String IdNumber;
}
