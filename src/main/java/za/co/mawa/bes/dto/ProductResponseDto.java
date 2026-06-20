package za.co.mawa.bes.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private String id;
    private String code;
    private String description;
    private String type;
    private String uom;
    private Date validFrom;
    private Date validTo;
}
