package za.co.mawa.bes.dto.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public final class PosPrintingDtos {
    private PosPrintingDtos() {
    }

    @Getter
    @Setter
    public static class EnrollmentCreateRequest {
        private String agentName;
        private String location;
        private Integer validMinutes;
    }

    @Getter
    @Setter
    @Builder
    public static class EnrollmentResponse {
        private String code;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime expiresAt;
        private String agentName;
        private String location;
    }

    @Getter
    @Setter
    public static class AgentEnrollRequest {
        private String code;
        private String machineName;
        private String osName;
        private String osVersion;
        private String agentVersion;
    }

    @Getter
    @Setter
    @Builder
    public static class AgentEnrollResponse {
        private String agentId;
        private String agentSecret;
        private String agentName;
        private String location;
    }

    @Getter
    @Setter
    public static class PrinterSyncItem {
        private String windowsQueueName;
        private String displayName;
        private Boolean defaultPrinter;
        private Boolean supportsCut;
        private Integer paperWidthChars;
    }

    @Getter
    @Setter
    public static class PrinterSyncRequest {
        private List<PrinterSyncItem> printers;
    }

    @Getter
    @Setter
    public static class HeartbeatRequest {
        private String machineName;
        private String osName;
        private String osVersion;
        private String agentVersion;
    }

    @Getter
    @Setter
    @Builder
    public static class PrinterResponse {
        private String id;
        private String agentId;
        private String windowsQueueName;
        private String displayName;
        private String printerRole;
        private String status;
        private boolean defaultPrinter;
        private boolean supportsCut;
        private Integer paperWidthChars;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime lastSeenAt;
    }

    @Getter
    @Setter
    @Builder
    public static class AgentResponse {
        private String id;
        private String name;
        private String machineName;
        private String location;
        private String status;
        private boolean online;
        private String agentVersion;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime lastHeartbeatAt;
        private List<PrinterResponse> printers;
    }

    @Getter
    @Setter
    public static class PrinterConfigurationRequest {
        private String displayName;
        private String printerRole;
        private Boolean supportsCut;
        private Integer paperWidthChars;
    }

    @Getter
    @Setter
    public static class TerminalRegisterRequest {
        private String terminalKey;
        private String displayName;
        private String location;
    }

    @Getter
    @Setter
    public static class TerminalAssignmentRequest {
        private String agentId;
        private String defaultReceiptPrinterId;
        private String defaultDocumentPrinterId;
    }

    @Getter
    @Setter
    @Builder
    public static class TerminalResponse {
        private String id;
        private String terminalKey;
        private String displayName;
        private String location;
        private String agentId;
        private String defaultReceiptPrinterId;
        private String defaultDocumentPrinterId;
        private boolean enabled;
    }

    @Getter
    @Setter
    public static class TerminalEnabledRequest {
        private Boolean enabled;
    }

    @Getter
    @Setter
    public static class QueueReceiptRequest {
        private String terminalId;
        private String terminalKey;
        private String printerId;
        private String requestId;
        private Boolean reprint;
    }

    @Getter
    @Setter
    @Builder
    public static class PrintJobResponse {
        private String id;
        private String sourceType;
        private String sourceId;
        private String receiptId;
        private String terminalId;
        private String agentId;
        private String printerId;
        private String printerQueueName;
        private Boolean printerSupportsCut;
        private Integer paperWidthChars;
        private String content;
        private String contentType;
        private String status;
        private String claimToken;
        private int attemptCount;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime claimExpiresAt;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime createdAt;
        private String lastError;
    }

    @Getter
    @Setter
    public static class JobResultRequest {
        private String claimToken;
        private String errorMessage;
    }
}
