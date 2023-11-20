package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@Getter
@Setter
public class IdentityDto implements Serializable {

    private String partner;
    private FieldOptionDto type;
    private String number;
    private String validFrom;
    private String validTo;

    private String status;


}
