package za.co.mawa.bes.dto.v2;

import za.co.mawa.bes.enums.ApprovalActionType;
import java.util.Date;

/** Immutable response shape; persisted audit actions are never rewritten. */
public record ApprovalActionView(String id, String approvalRequestId, Integer stepNo,
        ApprovalActionType action, String actionBy, String actionByName, Date actionAt, String comments) {}
