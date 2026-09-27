package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class InventoryAdjustmentApprovalHandler extends AbstractInventoryApprovalHandler {
    public InventoryAdjustmentApprovalHandler(ObjectProvider<InventoryManagementService> serviceProvider) { super(serviceProvider); }
    @Override public ApprovalType supports() { return ApprovalType.INVENTORY_ADJUSTMENT; }
}
