package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralPackageUpdateRequestDto {

    private String id;
    private String name;
    private Long basePriceCents;
    private String inclusionsJson;
    private Boolean active;
}
