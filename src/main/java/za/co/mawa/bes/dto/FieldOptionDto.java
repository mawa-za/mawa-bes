package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FieldOptionDto implements Serializable {
    private String field;
    private String code;
    private String type;
    private String description;
    private Date validFrom;
    private Date validTo;
}
