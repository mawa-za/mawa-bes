package za.co.mawa.bes.dto.sales.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class SalesOrderQueryDto {
    private String customerId;
    private String salesRepresentativeId;
    private Date expectedDate;
    private String status;
}
