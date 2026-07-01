package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralPackageResponseDto {

    private String id;
    private String name;
    private Long basePriceCents;
    private String inclusionsJson;
    private List<String> inclusions;
    private Boolean active;
}
