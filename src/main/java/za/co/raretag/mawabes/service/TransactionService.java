package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.TransactionDao;
import za.co.raretag.mawabes.dto.OrderDateDto;
import za.co.raretag.mawabes.entity.NumberRangeEntity;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.repository.TransactionRepository;
import za.co.raretag.mawabes.utils.Constant;
import za.co.raretag.mawabes.utils.Conversion;
import za.co.raretag.mawabes.utils.DateType;
import za.co.raretag.mawabes.utils.Status;

import java.util.Date;

@Service
public class TransactionService implements TransactionDao {

    @Autowired
    NumberRangeService numberRange;
    @Autowired
    TransactionRepository transactionRepository;
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
}
