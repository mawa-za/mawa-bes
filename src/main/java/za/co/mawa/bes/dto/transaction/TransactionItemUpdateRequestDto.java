package za.co.mawa.bes.dto.transaction;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItemUpdateRequestDto {

    private String product;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private Date validFrom;
    private Date validTo;
    private String unitOfMeasure;
    private String status;
}
