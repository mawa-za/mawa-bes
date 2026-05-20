package za.co.mawa.bes.service.v2;

import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.ApprovalType;

public interface MembershipUpdateHandler {
    void onUpdate(String entity);
}