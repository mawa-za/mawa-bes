package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class AddressEditDto implements Serializable {
    private String id;
    private String line1;
    private String line2;
    private String line3;
    private String line4;
    private String suburb;
    private String town;
    private String city;
    private String province;
    private String postalCode;
}
