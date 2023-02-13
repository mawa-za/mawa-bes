package za.co.mawa.bes.dto.sales.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class SalesOrderCreateDto implements Serializable {
private String customerId;

    private List<LineItemDto> items;
}
