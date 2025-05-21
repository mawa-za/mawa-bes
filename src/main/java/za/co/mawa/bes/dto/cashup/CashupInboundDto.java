package za.co.mawa.bes.dto.cashup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CashupInboundDto {
    private String employeeResponsibleId;
    private String salesArea;
    private String deviceId;
}
