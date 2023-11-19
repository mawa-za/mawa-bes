package za.co.mawa.bes.dto.partner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PartnerQueryDto {
    private String id;
    private String type;
    private String idType;
    private String idNumber;
    private String name1;
    private String name2;
    private String name3;
    private String cellphone;
    private String email;
    private String role;
    private String filter;

   private Date validFrom;
   private Date validTo;
   private String createdBy;
   private Date creationDate;
   private String number;

}
