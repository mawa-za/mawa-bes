package za.co.mawa.bes.dto.layby;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

public class LayByGetDto implements Serializable {
    private String id;
    private String number;
    private String status;
    private String statusReason;
    private BigDecimal amount;
    private String dateCreated;
    private String endDate;
    private PartnerDto customer;
}
