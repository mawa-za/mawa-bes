package za.co.mawa.bes.dto.quotation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class QuotationDto extends TransactionDto implements Serializable {
}
