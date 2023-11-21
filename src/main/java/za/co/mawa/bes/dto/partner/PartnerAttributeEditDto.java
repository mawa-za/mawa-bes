package za.co.mawa.bes.dto.partner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PartnerAttributeEditDto implements Serializable {
    private String value;
    private String validFrom;
    private String validTo;
}
