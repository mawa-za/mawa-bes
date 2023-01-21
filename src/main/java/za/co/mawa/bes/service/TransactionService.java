package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.MessageDto;
import za.co.mawa.bes.repository.TransactionDateRepository;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.dao.TransactionDao;
import za.co.mawa.bes.dto.OrderDateDto;
import za.co.mawa.bes.entity.TransactionDateEntity;
import za.co.mawa.bes.entity.TransactionDatePKEntity;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.ArrayList;
import java.util.Date;

@Service
public class TransactionService implements TransactionDao {

    @Autowired
    NumberRangeService numberRange;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionDateRepository transactionDateRepository;

    @Override
    public String create(TransactionEntity transactionEntity) {

        String txnNo = null;
        try {
            txnNo = numberRange.generateNumber(transactionEntity.getType());
            transactionEntity.setId(txnNo);
            if (transactionEntity.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionRepository.save(transactionEntity);
            OrderDateDto creationDate = new OrderDateDto();
            creationDate.setTransaction(txnNo);
            creationDate.setType(DateType.CREATED);
            addDate(creationDate);
        } catch (Exception ex) {
            return null;
        }

        return txnNo;
    }

    @Override
    public String update(TransactionEntity transactionEntity) {
        return null;
    }

    @Override
    public String delete(String id) {
        return null;
    }

    @Override
    public TransactionEntity findById(String id) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> addDate(OrderDateDto od) {

        TransactionDatePKEntity txnDatePK = new TransactionDatePKEntity();
        txnDatePK.setTransaction(od.getTransaction());
        txnDatePK.setType(od.getType());
        TransactionDateEntity txnDate = new TransactionDateEntity();
        txnDate.setTransactionDatePK(txnDatePK);
        if (od.getValue() != null) {
            txnDate.setValue(Conversion.stringToDate(od.getValue()));
        } else {
            txnDate.setValue(new Date());
        }
        try {
            transactionDateRepository.save(txnDate);
        } catch (Exception ex) {
            return null;
        }

        return null;
    }
}
