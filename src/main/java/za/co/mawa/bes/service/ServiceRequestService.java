package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.MessageDto;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.dto.OrderDateDto;
import za.co.mawa.bes.entity.TransactionEntity;
import za.co.mawa.bes.dao.TransactionDao;

import java.util.ArrayList;

@Service
public class ServiceRequestService implements TransactionDao {
    @Autowired
    TransactionRepository transactionRepository;


    @Override
    public String create(TransactionEntity transactionEntity) {
        return null;
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
        return null;
    }
}
