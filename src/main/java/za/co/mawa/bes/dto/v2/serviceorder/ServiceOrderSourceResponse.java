package za.co.mawa.bes.dto.v2.serviceorder;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceOrderSourceResponse {
    private String sourceType;
    private String sourceId;
    private String sourceNo;
}
