package za.co.mawa.bes.dto.invoice;

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
public class InvoiceCreateDto implements Serializable {
    private String customerId;
    private Date dueDate;
    private List<LineItemDto> items;

}
