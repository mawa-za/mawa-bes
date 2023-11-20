package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerIdentityEditDto implements Serializable {
    private String partner;
    private String type;
    private String number;
    private Date validFrom;
    private Date validTo;


}
