package za.co.mawa.bes.dto.invoice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class InvoiceDto implements Serializable {
    private String id;
    private FieldOptionDto type;
    private String number;
    private PartnerDto customer;
    private Date dueDate;
    private Date invoiceDate;
    private FieldOptionDto paymentMethod;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private List<LineItemOutboundDto> items;

}
