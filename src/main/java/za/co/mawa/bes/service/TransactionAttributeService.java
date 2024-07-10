package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeCreateDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.repository.TransactionAttributeRepository;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.TransactionAttribute;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class TransactionAttributeService {
    @Autowired
    TransactionAttributeRepository transactionAttributeRepository;
    @Autowired
    FieldOptionService fieldOptionService;

    public void add(TransactionAttributeDto transactionAttributeDto) {
        try {
            TransactionAttributeEntity transactionAttributeEntity = new TransactionAttributeEntity();
            transactionAttributeEntity.setTransaction(transactionAttributeDto.getTransaction());
            transactionAttributeEntity.setAttribute(transactionAttributeDto.getAttribute());
            transactionAttributeEntity.setValue(transactionAttributeDto.getValue());
            transactionAttributeEntity.setValidTo(new Date());
            transactionAttributeEntity.setValidFrom(new Date());
            transactionAttributeRepository.save(transactionAttributeEntity);
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public String get(TransactionAttributeDto transactionAttributeDto) {
        try {
            List<TransactionAttributeEntity> transactionAttributeEntityList = transactionAttributeRepository.find(transactionAttributeDto.getTransaction(), transactionAttributeDto.getAttribute());
            TransactionAttributeEntity transactionAttributeEntity = transactionAttributeEntityList.iterator().next();
            transactionAttributeDto.setValue(transactionAttributeEntity.getValue());
            return transactionAttributeDto.getValue();
        } catch (Exception exception) {
            return null;
        }
    }

    public List<TransactionAttributeEntity> getByTransactionId(String transactionId) {
        try {
            return transactionAttributeRepository.findAllByTransaction(transactionId);
        } catch (Exception exception) {
            return null;
        }
    }

    public void edit(TransactionAttributeDto transactionAttributeDto) {
        try {
            List<TransactionAttributeEntity> transactionAttributeEntityList = transactionAttributeRepository.find(transactionAttributeDto.getTransaction(), TransactionAttribute.LAST_PREMIUM_PERIOD);
            TransactionAttributeEntity transactionAttributeEntity = transactionAttributeEntityList.iterator().next();
            transactionAttributeEntity.setValue(transactionAttributeDto.getValue());
            transactionAttributeRepository.save(transactionAttributeEntity);
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public void delete(TransactionAttributeDto transactionAttributeDto) {
        try {
            List<TransactionAttributeEntity> transactionAttributeEntityList = transactionAttributeRepository.find(transactionAttributeDto.getTransaction(), TransactionAttribute.LAST_PREMIUM_PERIOD);
            TransactionAttributeEntity transactionAttributeEntity = transactionAttributeEntityList.iterator().next();
            transactionAttributeRepository.deleteById(transactionAttributeEntity.getId());
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }
}
