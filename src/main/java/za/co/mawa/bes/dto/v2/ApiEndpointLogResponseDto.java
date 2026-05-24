package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ApiEndpointLogResponseDto {

    private String id;
    private String requestId;

    private String userId;
    private String username;

    private String method;
    private String endpoint;
    private String queryString;

    private Integer statusCode;

    private String requestIp;
    private String userAgent;

    private Long durationMs;

    private Boolean success;
    private String errorMessage;

    private LocalDateTime createdAt;
}