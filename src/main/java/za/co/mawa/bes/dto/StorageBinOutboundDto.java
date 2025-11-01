package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class StorageBinOutboundDto implements Serializable {
    private String binId;
    private String warehouseId;
    private String aisle;
    private String rack;
    private String stack;
    private String shelf;
    private String description;
    private String product;
    private String binCode;
    private boolean published;
    private String batchNumber;
    private Date expiryDate;
}