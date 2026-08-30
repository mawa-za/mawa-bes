package za.co.mawa.bes.dto.v2.devicesync;

import lombok.Data;

@Data
public class DeviceSyncCancellationRequest {
    private String deviceId;
    private String idempotencyKey;
    private String reason;
}
