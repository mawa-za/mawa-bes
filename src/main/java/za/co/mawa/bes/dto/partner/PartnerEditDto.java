package za.co.mawa.bes.dto.partner;

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
public class PartnerEditDto implements Serializable {
    private String id;
    private String name1;
    private String name2;
    private String name3;
    private String gender;
    private String maritalStatus;
    private String title;
    private String language;
    private Date birthDate;
    private String status;
}
