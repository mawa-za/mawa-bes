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
public class UserUpdateRequestDto {

    private String id;
    private String username;
    private String partner;
    private String cellphone;
    private String email;
    private String passwordStatus;
    private String status;
    private String statusReason;
    private Date validFrom;
    private Date validTo;
    private String userType;
}
