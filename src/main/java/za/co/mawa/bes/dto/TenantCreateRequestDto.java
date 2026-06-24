package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantCreateRequestDto {

    private String name;
    private String url;
    private String host;
    private String status;
}
