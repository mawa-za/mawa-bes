package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class RepresentativeCreateDto implements Serializable {
    private String partnerId;
    private String dateAdded;
    private String dateEffective;
}
