package za.co.mawa.bes.dto.premium;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PremiumSearchDto implements Serializable{
    private String membershipId;
    private String membershipPeriod;
    private String tenderType;
    private String location;
    private String createdBy;
    private String employeeResponsible;
}
