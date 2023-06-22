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
public class TransactionAttachmentCreateDto implements Serializable {
    private String transaction;
    private String fileType;
    
}
