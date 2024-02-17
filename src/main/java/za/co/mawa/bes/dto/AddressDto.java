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
    private FieldOptionDto type;
    private String line1;
    private String line2;
    private FieldOptionDto line3;
    private FieldOptionDto line4;
    private FieldOptionDto suburb;
    private FieldOptionDto town;
    private FieldOptionDto city;
    private FieldOptionDto province;
    private String postalCode;

}
