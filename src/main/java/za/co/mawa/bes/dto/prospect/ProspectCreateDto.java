package za.co.mawa.bes.dto.prospect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProspectCreateDto implements Serializable{

    private String partnerType;
    private String surname;
    private String firstName;
    private String middleName;
    private String lastName;
    private String organisationName;
}
