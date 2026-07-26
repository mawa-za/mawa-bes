package za.co.mawa.bes.dto.product.classification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTypeDefinitionDto {
    private String code;
    private String name;
    private String description;
    private boolean stockControlled;
    private boolean canBeReceived;
    private boolean canBePutAway;
    private boolean consumedOnIssue;
    private boolean returnable;
    private boolean assetTracked;
    private boolean bundle;
    private boolean specialisedWorkflow;
    private boolean defaultAvailableForSale;
}
