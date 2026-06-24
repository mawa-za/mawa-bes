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
public class TransactionUpdateRequestDto {

    private String id;
    private String number;
    private String type;
    private String subType;
    private String description;
    private Date validFrom;
    private Date validTo;
    private String status;
    private String statusReason;
    private String subStatus;
    private String location;
    private String category;
    private String subDescription;
    private String changedBy;
    private String priority;
}
