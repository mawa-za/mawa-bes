package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionAttachmentDto implements Serializable {
    private String transaction;
    private String fileType;
    private String fileId;
    private String validFrom;
    private String validTo;
    private String status;
}
