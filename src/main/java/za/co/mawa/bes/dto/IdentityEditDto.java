package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class IdentityEditDto implements Serializable {
    private String idNumber;
    private String validTo;
    private String validFrom;
}
