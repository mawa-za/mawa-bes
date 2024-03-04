package za.co.mawa.bes.dto.invoice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class InvoiceQueryDto {
    private String number;
    private String status;
    private String customer;
    private Date dueDate;
}
