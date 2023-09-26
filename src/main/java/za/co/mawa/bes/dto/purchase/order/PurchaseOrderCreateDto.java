package za.co.mawa.bes.dto.purchase.order;

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
public class PurchaseOrderCreateDto implements Serializable {
    private String supplierId;
    private Date orderDate;
    private Date expectedDate;
    private String deliveryAddress;
    private List<LineItemDto> items;
    private String paymentMethod;
    private String customerId;
}
