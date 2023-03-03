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
public class SalesOrderDto implements Serializable {
    private String id;
    private String number;
    private String customerId;
    private String salesRepresentativeId;
    private Date orderDate;
    private Date expectedDate;
    private Date deliveryDate;
    private List<LineItemDto> items;
}
