package za.co.mawa.bes.dto.cashup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CashupCreateDto {
    private String employeeResponsibleId;
    private String salesArea;
    private BigDecimal amount;
    private List<String> receipts;
    private CashUpType cashUpType;

    public enum CashUpType {
        AUTOMATIC,
        MANUAL,
        RECEIPT
    }

}
