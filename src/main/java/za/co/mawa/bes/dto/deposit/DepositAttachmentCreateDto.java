package za.co.mawa.bes.dto.deposit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DepositAttachmentCreateDto {
    private String amount;
    private String transactionIdLink;
    private String file;
    private String extension;
    private String documentType;
    private String objectType;

}
