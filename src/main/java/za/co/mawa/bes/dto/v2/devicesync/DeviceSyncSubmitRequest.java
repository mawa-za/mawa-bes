package za.co.mawa.bes.dto.v2.devicesync;
import lombok.Data;
@Data public class DeviceSyncSubmitRequest { private String submissionId; private String idempotencyKey; private String deviceId; private String method; private String path; private Object payload; }
