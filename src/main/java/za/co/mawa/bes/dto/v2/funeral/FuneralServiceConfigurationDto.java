package za.co.mawa.bes.dto.v2.funeral;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralServiceConfigurationDto {
    /**
     * Maximum number of membership covers that can be selected for one funeral service.
     * A value of 0 means unlimited, preserving legacy behaviour until a tenant configures a limit.
     */
    private Integer maxSelectableCovers;

    private Boolean coverSelectionLimitEnabled;
    private Boolean automaticMortuaryCheckoutEnabled;
}
