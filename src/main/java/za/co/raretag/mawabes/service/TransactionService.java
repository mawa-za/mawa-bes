package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.TransactionDao;
import za.co.raretag.mawabes.dto.MessageDto;
import za.co.raretag.mawabes.dto.OrderDateDto;
import za.co.raretag.mawabes.entity.NumberRangeEntity;
import za.co.raretag.mawabes.entity.TransactionDateEntity;
import za.co.raretag.mawabes.entity.TransactionDatePKEntity;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.repository.TransactionDateRepository;
import za.co.raretag.mawabes.repository.TransactionRepository;
import za.co.raretag.mawabes.utils.Constant;
import za.co.raretag.mawabes.utils.Conversion;
import za.co.raretag.mawabes.utils.DateType;
import za.co.raretag.mawabes.utils.Status;

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
