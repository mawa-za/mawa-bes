package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MembershipCreateRequest implements Serializable {
    private String memberId;
    private String planId;
    private String startDate;
    private String joinDate;
    private String status;
}