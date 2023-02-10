package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class QuotationCreateDto implements Serializable {
    private String customerId;
    private Date deliveryDate;
    private Date expiryDate;

    private List<ItemDto> items;
}
