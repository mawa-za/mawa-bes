package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRelationPKResponseDto {

    private String type;
    private String partner1;
    private String partner2;
}
