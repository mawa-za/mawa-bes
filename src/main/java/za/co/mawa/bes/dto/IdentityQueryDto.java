package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class IdentityQueryDto implements Serializable {
    private String partner;
    private String type;
    private String value;

}
