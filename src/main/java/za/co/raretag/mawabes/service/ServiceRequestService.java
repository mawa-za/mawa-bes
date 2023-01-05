package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.model.TransactionDao;
import za.co.raretag.mawabes.repository.TransactionRepository;

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
}
