package za.co.mawa.bes.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookingDto implements Serializable {
    private PartnerDto customer;
    private PartnerDto employeeResponsible;
    private String bookDate;
    private String bookTime;
    private String duration;
    private ProductDto productDto;
    private String id;
    private String number;
    private String status;
    private String createdOn;
}
