package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class StorageBinInboundDto implements Serializable {
    private String binId;
    private String warehouseId;
    private String aisle;
    private String rack;
    private String shelf;
    private String description;
    private String product;
    private String binCode;
    private String published;
}
