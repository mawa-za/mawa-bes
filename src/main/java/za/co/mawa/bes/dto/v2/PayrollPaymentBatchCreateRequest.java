package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PayrollPaymentBatchCreateRequest {

    private String batchNo;

    private String description;

    private String payPeriod;

    private LocalDate paymentDate;

    private String notes;

    private List<PayrollPaymentItemRequest> items;
}