package za.co.mawa.bes.dto.sales.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class SalesOrderCreateDto implements Serializable {
    private String customerId;
    private String salesRepresentativeId;
    private String description;
    private Date expectedDate;
    private List<LineItemDto> items;
}
