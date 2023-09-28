package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestDto implements Serializable {
    private String id;
    private String no;
    private String subType;
    private String customerId;
    private String customerName;
    private String description;
    private String category;
    private String priority;
    private String status;

    public ServiceRequestDto(TransactionDto transactionDto) {
        this.id = transactionDto.getId();
        this.no = transactionDto.getNo();
        this.description = transactionDto.getDescription();
        this.category = transactionDto.getSubType();
        this.subType = transactionDto.getSubType();
        this.priority = transactionDto.getPriority();
        this.customerId = transactionDto.getCustomerId();
        this.customerName = transactionDto.getCustomerName();
        this.status = transactionDto.getStatus();
    }
}
