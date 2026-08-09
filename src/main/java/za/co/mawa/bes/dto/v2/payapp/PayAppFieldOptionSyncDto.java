package za.co.mawa.bes.dto.v2.payapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayAppFieldOptionSyncDto {
    private String field;
    private String code;
    private String description;
}
