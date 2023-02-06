package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class QuotationCreateDto implements Serializable {
    private String customer;
    private Date deliveryDate;
    private Date expiryDate;
}
