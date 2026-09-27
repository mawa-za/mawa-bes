package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class SupplierReturnApprovalHandler extends AbstractInventoryApprovalHandler {
    public SupplierReturnApprovalHandler(ObjectProvider<InventoryManagementService> serviceProvider) { super(serviceProvider); }
    @Override public ApprovalType supports() { return ApprovalType.SUPPLIER_RETURN; }
}
