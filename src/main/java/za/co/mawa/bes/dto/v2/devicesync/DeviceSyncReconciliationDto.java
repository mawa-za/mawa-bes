package za.co.mawa.bes.dto.v2.devicesync;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class DeviceSyncReconciliationDto {
    private boolean verified;
    private String entityType;
    private String serverRecordId;
    private String serverStatus;
    private Map<String, String> receiptIdsByNumber;
    private String message;
}
