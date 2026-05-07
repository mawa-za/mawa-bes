package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PayrollPaymentBatchCopyRequest {

    private String batchNo;

    private String description;

    private String payPeriod;

    private LocalDate paymentDate;

    private String notes;

    private Boolean copyExcludedItems = false;
}