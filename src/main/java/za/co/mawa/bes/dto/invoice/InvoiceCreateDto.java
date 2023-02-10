package za.co.mawa.bes.dto.invoice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.ItemDto;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class InvoiceCreateDto implements Serializable {
private String customer;
private List<ItemDto> items;

}
