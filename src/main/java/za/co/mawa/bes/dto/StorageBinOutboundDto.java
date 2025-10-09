package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

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
    private String productId;
    private String binCode;
    private boolean published;
}