package za.co.mawa.bes.dto.cas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CaseQueryDto {
    private String number;
    private String customer;
    private String service;
    private Date dateCreated;
    private String status;
}
