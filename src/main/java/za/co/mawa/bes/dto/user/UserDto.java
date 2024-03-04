package za.co.mawa.bes.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class UserDto implements Serializable {
    private String id;
    private String username;
    private String password;
    private String email;
    private String cellphone;
    private String type;
    private String status;
    private PartnerDto partner;
    private String passwordStatus;
    private Date validFrom;
    private Date validTo;
    private String statusReason;

}
