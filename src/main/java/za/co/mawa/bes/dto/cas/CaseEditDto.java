package za.co.mawa.bes.dto.cas;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CaseEditDto implements Serializable{
    private String status;
    private String statusReason;
    private Date courtDate;
}
