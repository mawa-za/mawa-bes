package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.transaction.amount.*;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.repository.TransactionAmountRepository;
import za.co.mawa.bes.utils.AmountType;
import za.co.mawa.bes.utils.Field;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
public class TransactionAmountService {
    @Autowired
    TransactionAmountRepository transactionAmountRepository;
    @Autowired
    FieldOptionService fieldOptionService;

    public TransactionAmountOutboundDto getById(String id) {
        TransactionAmountOutboundDto transactionAmountOutboundDto = new TransactionAmountOutboundDto();
        TransactionAmountEntity transactionAmountEntity = transactionAmountRepository.getById(id);
        transactionAmountOutboundDto.setId(transactionAmountEntity.getId());
        transactionAmountOutboundDto.setTransaction(transactionAmountEntity.getTransaction());
        transactionAmountOutboundDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_AMOUNT, transactionAmountEntity.getType()));
        transactionAmountOutboundDto.setAmount(transactionAmountEntity.getAmount());
        transactionAmountOutboundDto.setCreatedBy(transactionAmountEntity.getCreatedBy());
        transactionAmountOutboundDto.setChangedBy(transactionAmountEntity.getChangedBy());
        return transactionAmountOutboundDto;
    }

    public List<TransactionAmountOutboundDto> getByTransaction(String transaction) {
        List<TransactionAmountOutboundDto> transactionAmountOutboundDtos = new ArrayList<>();
        List<TransactionAmountEntity> transactionAmountEntities = transactionAmountRepository.getByTransaction(transaction);
        for (TransactionAmountEntity transactionAmountEntity : transactionAmountEntities) {
            transactionAmountOutboundDtos.add(getById(transactionAmountEntity.getId()));
        }
        return transactionAmountOutboundDtos;
    }

    public void save(TransactionAmountInboundDto transactionAmountInboundDto) {
        TransactionAmountEntity transactionAmountEntity;
        try {
            Iterator iterator = transactionAmountRepository
                    .getByTransaction(transactionAmountInboundDto.getTransaction()).stream()
                    .filter(a -> Objects.equals(a.getType(), transactionAmountInboundDto.getType()))
                    .toList().iterator();
            if (!iterator.hasNext()) {
                transactionAmountEntity = new TransactionAmountEntity();
            } else {
                transactionAmountEntity = (TransactionAmountEntity) iterator.next();
            }
            transactionAmountEntity.setTransaction(transactionAmountInboundDto.getTransaction());
            transactionAmountEntity.setType(transactionAmountInboundDto.getType());
            transactionAmountEntity.setAmount(transactionAmountInboundDto.getAmount());
            if (transactionAmountInboundDto.getId() == null) {
                transactionAmountEntity.setCreatedBy(UserContext.getCurrentUser());
            } else {
                transactionAmountEntity.setChangedBy(UserContext.getCurrentUser());
            }
            transactionAmountRepository.save(transactionAmountEntity);
        } catch (Exception ex) {

        }

    }
}
