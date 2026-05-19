package za.co.mawa.bes.dto.v2;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PayrollPaymentBatchEditRequest {
    private String batchNo;
    private String description;
    private String payPeriod;
    private LocalDate paymentDate;
    private String notes;
    private List<PayrollPaymentItemEditRequest> items;
}