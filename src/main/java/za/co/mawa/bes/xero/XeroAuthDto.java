package za.co.mawa.bes.xero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class XeroAuthDto {
    private String redirectUrl;
    private String clientId;
    private String clientSecret;
}
