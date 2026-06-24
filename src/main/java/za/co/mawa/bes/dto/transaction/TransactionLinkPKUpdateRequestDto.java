package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLinkPKUpdateRequestDto {

    private String transaction1;
    private String transaction2;
    private String type;
}
