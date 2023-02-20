package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class DependentDto implements Serializable {
    private String id;
    private String title;
    private String idType;
    private String idNumber;
    private String lastName;
    private String firstName;
    private String middleName;
    private String gender;
}
