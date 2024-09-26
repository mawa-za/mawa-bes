package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionViewDto {
    private String customerName;
    private String status;
    private String idNumber;
    private String employeeResponsibleName;
    private Date creationDate;
    private String type;
}
