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
public class LayByQueryDto implements Serializable {
    private String number;
    private String customerId;
    private String salesRepresentativeId;
    private String endDate;
    private String dateCreated;
    private String status;
}
