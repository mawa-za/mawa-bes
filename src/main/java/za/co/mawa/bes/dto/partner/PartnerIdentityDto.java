package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerIdentityDto implements Serializable {
    private String partner;
    private FieldOptionDto type;
    private String number;
    private Date validFrom;
    private Date validTo;


}
