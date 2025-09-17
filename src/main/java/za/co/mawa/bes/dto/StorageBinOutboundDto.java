package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class StorageBinOutboundDto implements Serializable {
    private String aisle;
    private String rack;
    private String shelf;
    private String description;
    private String product;
    private boolean published;
}