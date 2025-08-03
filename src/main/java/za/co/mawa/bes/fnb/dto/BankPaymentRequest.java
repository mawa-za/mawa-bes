package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class BankPaymentRequest implements Serializable {
    private GroupHeader groupHeader;
    private List<PaymentInformation> paymentInformation;
}