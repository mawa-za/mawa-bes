package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ContactGetDto implements Serializable {
    private String partner;
    private String type;
    private String description;
    private  String value;
    private String validFrom;
    private String validTo;

}
