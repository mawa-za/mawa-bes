package za.co.mawa.bes.dto.group.society;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GroupSocietyCreateDto implements Serializable {
    private String customer;
    private String salesRepresentative;
    private String product;
    private String salesArea;
    private Date dateJoined;
    private String creationType;
    private BigDecimal openingBalance;
    private BigDecimal totalDeposited;
    private BigDecimal totalWithdrawn;
}
