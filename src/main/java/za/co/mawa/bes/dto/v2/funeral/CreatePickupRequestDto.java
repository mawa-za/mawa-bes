package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePickupRequestDto {
    private String deceasedName;
    private String pickupLocation;
    private String contactPerson;
    private String contactNumber;
}
