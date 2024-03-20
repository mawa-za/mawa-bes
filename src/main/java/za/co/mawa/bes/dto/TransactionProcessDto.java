package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionProcessDto implements Serializable {
    private String id;
    private String action;
    private String status;
    private String reason;
    private String notes;

}
