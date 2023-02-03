package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionQueryDto implements Serializable {
    private String type;
    private String subtype;
    private String status;
    private String partnerFunction;
    private String partnerNo;

}
