package za.co.mawa.bes.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageBinUpdateRequestDto {

    private String binId;
    private String warehouseId;
    private String description;
    private String aisle;
    private String stack;
    private String shelf;
    private String binCode;
    private String binType;
    private BigDecimal capacity;
    private String unitOfMeasure;
    private String status;
    private Boolean published;
    private String productId;
    private String batchNumber;
    private Date expiryDate;
}
