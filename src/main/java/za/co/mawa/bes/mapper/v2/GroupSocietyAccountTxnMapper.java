package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;
import za.co.mawa.bes.dto.v2.GroupSocietyAccountTxnCreateRequestDto;
import za.co.mawa.bes.dto.v2.GroupSocietyAccountTxnResponseDto;
import za.co.mawa.bes.dto.v2.GroupSocietyAccountTxnUpdateRequestDto;

@Component
public class GroupSocietyAccountTxnMapper {

    public GroupSocietyAccountTxnResponseDto toResponse(GroupSocietyAccountTxnEntity entity) {
        if (entity == null) {
            return null;
        }

        return GroupSocietyAccountTxnResponseDto.builder()
                .id(entity.getId())
                .groupSocietyId(entity.getGroupSocietyId())
                .txnType(entity.getTxnType())
                .direction(entity.getDirection())
                .amountCents(entity.getAmountCents())
                .balanceBeforeCents(entity.getBalanceBeforeCents())
                .balanceAfterCents(entity.getBalanceAfterCents())
                .txnDate(entity.getTxnDate())
                .txnDatetime(entity.getTxnDatetime())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .referenceNo(entity.getReferenceNo())
                .paymentMethod(entity.getPaymentMethod())
                .period(entity.getPeriod())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public GroupSocietyAccountTxnEntity toEntity(GroupSocietyAccountTxnCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return GroupSocietyAccountTxnEntity.builder()
                .groupSocietyId(request.getGroupSocietyId())
                .txnType(request.getTxnType())
                .direction(request.getDirection())
                .amountCents(request.getAmountCents())
                .balanceBeforeCents(request.getBalanceBeforeCents())
                .balanceAfterCents(request.getBalanceAfterCents())
                .txnDate(request.getTxnDate())
                .txnDatetime(request.getTxnDatetime())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .referenceNo(request.getReferenceNo())
                .paymentMethod(request.getPaymentMethod())
                .period(request.getPeriod())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(GroupSocietyAccountTxnEntity entity, GroupSocietyAccountTxnUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setGroupSocietyId(request.getGroupSocietyId());
        entity.setTxnType(request.getTxnType());
        entity.setDirection(request.getDirection());
        entity.setAmountCents(request.getAmountCents());
        entity.setBalanceBeforeCents(request.getBalanceBeforeCents());
        entity.setBalanceAfterCents(request.getBalanceAfterCents());
        entity.setTxnDate(request.getTxnDate());
        entity.setTxnDatetime(request.getTxnDatetime());
        entity.setReferenceType(request.getReferenceType());
        entity.setReferenceId(request.getReferenceId());
        entity.setReferenceNo(request.getReferenceNo());
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setPeriod(request.getPeriod());
        entity.setNotes(request.getNotes());
    }
}
