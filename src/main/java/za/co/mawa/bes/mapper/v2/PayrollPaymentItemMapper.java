package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.PayrollPaymentItemEntity;
import za.co.mawa.bes.dto.v2.PayrollPaymentItemCreateRequestDto;
import za.co.mawa.bes.dto.v2.PayrollPaymentItemResponseDto;
import za.co.mawa.bes.dto.v2.PayrollPaymentItemUpdateRequestDto;

@Component
public class PayrollPaymentItemMapper {

    public PayrollPaymentItemResponseDto toResponse(PayrollPaymentItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return PayrollPaymentItemResponseDto.builder()
                .id(entity.getId())
                .batchId(entity.getBatchId())
                .employeeId(entity.getEmployeeId())
                .employeeNo(entity.getEmployeeNo())
                .employeeName(entity.getEmployeeName())
                .bankName(entity.getBankName())
                .branchCode(entity.getBranchCode())
                .accountNo(entity.getAccountNo())
                .accountType(entity.getAccountType())
                .accountHolderName(entity.getAccountHolderName())
                .amountCents(entity.getAmountCents())
                .paymentReference(entity.getPaymentReference())
                .salaryReference(entity.getSalaryReference())
                .status(entity.getStatus())
                .excluded(entity.getExcluded())
                .exclusionReason(entity.getExclusionReason())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public PayrollPaymentItemEntity toEntity(PayrollPaymentItemCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PayrollPaymentItemEntity.builder()
                .batchId(request.getBatchId())
                .employeeId(request.getEmployeeId())
                .employeeNo(request.getEmployeeNo())
                .employeeName(request.getEmployeeName())
                .bankName(request.getBankName())
                .branchCode(request.getBranchCode())
                .accountNo(request.getAccountNo())
                .accountType(request.getAccountType())
                .accountHolderName(request.getAccountHolderName())
                .amountCents(request.getAmountCents())
                .paymentReference(request.getPaymentReference())
                .salaryReference(request.getSalaryReference())
                .status(request.getStatus())
                .excluded(request.getExcluded())
                .exclusionReason(request.getExclusionReason())
                .failureReason(request.getFailureReason())
                .build();
    }

    public void updateEntity(PayrollPaymentItemEntity entity, PayrollPaymentItemUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setBatchId(request.getBatchId());
        entity.setEmployeeId(request.getEmployeeId());
        entity.setEmployeeNo(request.getEmployeeNo());
        entity.setEmployeeName(request.getEmployeeName());
        entity.setBankName(request.getBankName());
        entity.setBranchCode(request.getBranchCode());
        entity.setAccountNo(request.getAccountNo());
        entity.setAccountType(request.getAccountType());
        entity.setAccountHolderName(request.getAccountHolderName());
        entity.setAmountCents(request.getAmountCents());
        entity.setPaymentReference(request.getPaymentReference());
        entity.setSalaryReference(request.getSalaryReference());
        entity.setStatus(request.getStatus());
        entity.setExcluded(request.getExcluded());
        entity.setExclusionReason(request.getExclusionReason());
        entity.setFailureReason(request.getFailureReason());
    }
}
