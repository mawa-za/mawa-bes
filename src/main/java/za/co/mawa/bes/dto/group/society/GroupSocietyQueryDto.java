package za.co.mawa.bes.dto.group.society;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductBasicDto;

import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GroupSocietyQueryDto {
    private String id;
    private String number;
    private String customer;
    private String salesRepresentative;
    private String product;
    private Date dateCreated;
    private String status;
    private String statusReason;
}
