package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@Getter
@Setter
public class IdentityDto implements Serializable {
    private String validFrom;
    private String validTo;
    private String partner;
    private String idType;
    private String idNumber;
    private String typeDescription;


}
