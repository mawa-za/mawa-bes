package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public final class SystemInstallationFileDtos {
    private SystemInstallationFileDtos() {}

    @Data
    public static class UploadRequest {
        private String displayName;
        private String description;
        private String fileName;
        private String extension;
        private String file;
    }

    @Data
    @Builder
    public static class Response {
        private String id;
        private String displayName;
        private String description;
        private String fileName;
        private String extension;
        private LocalDateTime createdAt;
        private String createdBy;
    }

    @Data
    @Builder
    public static class DownloadResponse {
        private String fileName;
        private String extension;
        private String file;
    }
}
