package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRelationPKUpdateRequestDto {

    private String type;
    private String partner1;
    private String partner2;
}
