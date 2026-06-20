package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPartnerPKUpdateRequestDto {

    private String transaction;
    private String function;
    private String partner;
}
