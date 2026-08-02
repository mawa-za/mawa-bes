package za.co.mawa.bes.service.v2;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.enums.ApprovalType;
@Component
public class EmployeeSuspensionApprovalHandler extends AbstractEmploymentActionApprovalHandler {
    public EmployeeSuspensionApprovalHandler(EmploymentLifecycleService service) { super(service); }
    @Override public ApprovalType supports() { return ApprovalType.EMPLOYEE_SUSPENSION; }
}
