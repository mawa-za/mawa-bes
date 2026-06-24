package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointLogUpdateRequestDto {

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
    private String requestBody;
    private String responseBody;
    private Long durationMs;
    private Boolean success;
    private String errorMessage;
}
