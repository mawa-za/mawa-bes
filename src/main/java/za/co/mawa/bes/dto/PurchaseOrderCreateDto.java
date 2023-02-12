package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PurchaseOrderCreateDto implements Serializable {
    private String supplierId;
    private List<LineItemDto> items;
}
