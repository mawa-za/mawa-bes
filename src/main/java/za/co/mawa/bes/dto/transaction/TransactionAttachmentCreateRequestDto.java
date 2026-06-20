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
public class TransactionAttachmentCreateRequestDto {

    private String fileId;
    private Date validFrom;
    private Date validTo;
    private String status;
}
