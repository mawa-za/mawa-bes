package za.co.mawa.bes.dto.quotation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.LineItemInboundDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class QuotationCreateDto implements Serializable {
    private String customerId;
    private Date deliveryDate;
    private Date quotationDate;
    private String deliveryAddress;
    private Date expiryDate;
    private List<LineItemInboundDto> items;
}
