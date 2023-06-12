package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ContactDto implements Serializable {
    private String partner;
    private String type;
    private String detail;
    private String typeDescription;

    private Date validFrom;

    private Date validTo;
    private  String value;


}
