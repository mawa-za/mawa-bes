package za.co.mawa.bes.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerResourceApiResponseDto {

    private String resource_id;
    private String partner_url;
    private String partner_no;
    private Date validFrom;
    private Date validTo;
    private String status;
    private String status_reason;
    private String port_number;
    private String resource_name;
}
