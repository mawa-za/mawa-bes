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
public class LayByCreateDto implements Serializable {
    private String customerId;
    private String salesRepresentativeId;
    private int period;
    private String productId;

}
