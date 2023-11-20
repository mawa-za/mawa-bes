package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@Getter
@Setter
public class AddressDto implements Serializable {
    private String id;
    private String partner;
    private String type;
    private String line1;
    private String line2;
    private String line3;
    private String line4;
    private String postalCode;
    private String typeDescription;
    private String line3Description;
    private String line4Description;


}
