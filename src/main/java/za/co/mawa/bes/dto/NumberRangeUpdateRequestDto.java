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
public class NumberRangeUpdateRequestDto {

    private Integer id;
    private String object;
    private String prefix;
    private String start;
    private String current;
    private String end;
    private Date validFrom;
    private Date validTo;
}
