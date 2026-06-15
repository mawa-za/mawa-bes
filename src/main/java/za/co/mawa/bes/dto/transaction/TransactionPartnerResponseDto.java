package za.co.mawa.bes.dto.transaction;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPartnerResponseDto {

    private Date dateAdded;
    private Date dateEffective;
    private Date validFrom;
    private Date validTo;
    private String status;
    private String statusReason;
    private String createdBy;
    private String changedBy;
}
