package za.co.mawa.bes.dto.deposit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DepositSearchDto implements Serializable {
    private String createdBy;
    private String status;
    private String createdOn;

}
