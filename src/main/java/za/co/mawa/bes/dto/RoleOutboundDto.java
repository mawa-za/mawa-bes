package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RoleOutboundDto {
    private String id;
    private String name;
    private String description;
    private Date validFrom;
    private Date validTo;

}
