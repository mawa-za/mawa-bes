package za.co.mawa.bes.dto.purchase.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PurchaseOrderCreateDto implements Serializable {
    private String supplierId;
    private List<LineItemDto> items;
}
