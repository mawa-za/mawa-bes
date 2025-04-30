package za.co.mawa.bes.dto.transaction.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

public class TransactionItemEditDto implements Serializable {
    private String baseUnitOfMeasure;
    private String transaction;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private Date validFrom;
    private Date validTo;
    private String product;
    private String previousProduct;
    private String status;
    private String item;

}
