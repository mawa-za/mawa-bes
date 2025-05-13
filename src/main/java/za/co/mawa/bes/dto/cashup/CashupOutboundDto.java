package za.co.mawa.bes.dto.cashup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CashupOutboundDto {
    private String employeeResponsibleId;
    private String salesArea;
    private String deviceId;
}
