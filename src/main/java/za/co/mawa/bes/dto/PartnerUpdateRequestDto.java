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
public class PartnerUpdateRequestDto {

    private String id;
    private String no;
    private Date birthDate;
    private String gender;
    private String language;
    private String maritalStatus;
    private String name1;
    private String name2;
    private String name3;
    private String title;
    private String type;
    private Date validFrom;
    private Date validTo;
    private String status;
    private String statusReason;
    private Date creationDate;
}
