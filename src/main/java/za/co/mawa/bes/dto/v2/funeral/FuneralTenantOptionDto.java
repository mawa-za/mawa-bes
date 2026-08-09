package za.co.mawa.bes.dto.v2.funeral;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuneralTenantOptionDto {
    private String id;
    private String name;
    private String host;
    private String status;
}
