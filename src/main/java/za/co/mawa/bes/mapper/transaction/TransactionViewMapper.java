package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.dto.transaction.TransactionViewCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionViewResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionViewUpdateRequestDto;

@Component
public class TransactionViewMapper {

    public TransactionViewResponseDto toResponse(TransactionViewEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionViewResponseDto.builder()
                .transactionId(entity.getTransactionId())
                .transactionType(entity.getTransactionType())
                .transactionSubtype(entity.getTransactionSubtype())
                .transactionNumber(entity.getTransactionNumber())
                .identityType(entity.getIdentityType())
                .identityNumber(entity.getIdentityNumber())
                .mainPartnerId(entity.getMainPartnerId())
                .mainPartner(entity.getMainPartner())
                .employeeResponsibleId(entity.getEmployeeResponsibleId())
                .employeeResponsible(entity.getEmployeeResponsible())
                .recipientId(entity.getRecipientId())
                .recipient(entity.getRecipient())
                .createdById(entity.getCreatedById())
                .createdBy(entity.getCreatedBy())
                .changedById(entity.getChangedById())
                .changedBy(entity.getChangedBy())
                .product(entity.getProduct())
                .creationDate(entity.getCreationDate())
                .dueDate(entity.getDueDate())
                .deathDate(entity.getDeathDate())
                .burialDate(entity.getBurialDate())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .reference(entity.getReference())
                .batchNumber(entity.getBatchNumber())
                .transactionStatus(entity.getTransactionStatus())
                .claimant(entity.getClaimant())
                .amount(entity.getAmount())
                .amountCollected(entity.getAmountCollected())
                .amountDeposited(entity.getAmountDeposited())
                .dateEffective(entity.getDateEffective())
                .productId(entity.getProductId())
                .build();
    }

    public TransactionViewEntity toEntity(TransactionViewCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionViewEntity.builder()
                .transactionId(request.getTransactionId())
                .transactionType(request.getTransactionType())
                .transactionSubtype(request.getTransactionSubtype())
                .transactionNumber(request.getTransactionNumber())
                .identityType(request.getIdentityType())
                .identityNumber(request.getIdentityNumber())
                .mainPartnerId(request.getMainPartnerId())
                .mainPartner(request.getMainPartner())
                .employeeResponsibleId(request.getEmployeeResponsibleId())
                .employeeResponsible(request.getEmployeeResponsible())
                .recipientId(request.getRecipientId())
                .recipient(request.getRecipient())
                .createdById(request.getCreatedById())
                .changedById(request.getChangedById())
                .changedBy(request.getChangedBy())
                .product(request.getProduct())
                .creationDate(request.getCreationDate())
                .dueDate(request.getDueDate())
                .deathDate(request.getDeathDate())
                .burialDate(request.getBurialDate())
                .category(request.getCategory())
                .priority(request.getPriority())
                .reference(request.getReference())
                .batchNumber(request.getBatchNumber())
                .transactionStatus(request.getTransactionStatus())
                .claimant(request.getClaimant())
                .amount(request.getAmount())
                .amountCollected(request.getAmountCollected())
                .amountDeposited(request.getAmountDeposited())
                .dateEffective(request.getDateEffective())
                .productId(request.getProductId())
                .build();
    }

    public void updateEntity(TransactionViewEntity entity, TransactionViewUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransactionId(request.getTransactionId());
        entity.setTransactionType(request.getTransactionType());
        entity.setTransactionSubtype(request.getTransactionSubtype());
        entity.setTransactionNumber(request.getTransactionNumber());
        entity.setIdentityType(request.getIdentityType());
        entity.setIdentityNumber(request.getIdentityNumber());
        entity.setMainPartnerId(request.getMainPartnerId());
        entity.setMainPartner(request.getMainPartner());
        entity.setEmployeeResponsibleId(request.getEmployeeResponsibleId());
        entity.setEmployeeResponsible(request.getEmployeeResponsible());
        entity.setRecipientId(request.getRecipientId());
        entity.setRecipient(request.getRecipient());
        entity.setCreatedById(request.getCreatedById());
        entity.setChangedById(request.getChangedById());
        entity.setChangedBy(request.getChangedBy());
        entity.setProduct(request.getProduct());
        entity.setCreationDate(request.getCreationDate());
        entity.setDueDate(request.getDueDate());
        entity.setDeathDate(request.getDeathDate());
        entity.setBurialDate(request.getBurialDate());
        entity.setCategory(request.getCategory());
        entity.setPriority(request.getPriority());
        entity.setReference(request.getReference());
        entity.setBatchNumber(request.getBatchNumber());
        entity.setTransactionStatus(request.getTransactionStatus());
        entity.setClaimant(request.getClaimant());
        entity.setAmount(request.getAmount());
        entity.setAmountCollected(request.getAmountCollected());
        entity.setAmountDeposited(request.getAmountDeposited());
        entity.setDateEffective(request.getDateEffective());
        entity.setProductId(request.getProductId());
    }
}
