package za.co.mawa.bes.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressUpdateRequestDto {

    private String id;
    private String type;
    private String objectId;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressLine4;
    private String suburb;
    private String town;
    private String city;
    private String province;
    private String postalCode;
    private Date validFrom;
    private Date validTo;
}
