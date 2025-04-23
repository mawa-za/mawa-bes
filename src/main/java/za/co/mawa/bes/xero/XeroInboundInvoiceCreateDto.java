package za.co.mawa.bes.xero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class XeroInboundInvoiceCreateDto {
    private String username;
    private String password;
    private String partnerId;
    private String reference;
    private String itemCode;
}
