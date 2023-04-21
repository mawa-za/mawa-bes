package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
@NoArgsConstructor
@Getter
@Setter
public class EmploymentDto implements Serializable {
    private String employeeId;
    private String type;
    private String startDate;
    private String endDate;
    private String position;
    private String status;
    private String branch;
    private String department;
    private String groupID;
    private String groupName;

    private ArrayList<AddressDto> addressDtos;

    private ArrayList<IdentityDto> identityDtos;
    private ArrayList<ContactDto> contactDtos;


}
