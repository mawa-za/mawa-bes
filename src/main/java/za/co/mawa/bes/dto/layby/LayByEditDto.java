package za.co.mawa.bes.dto.layby;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LayByEditDto implements Serializable {
    private String status;
    private String statusReason;
    private String endDate;
}
