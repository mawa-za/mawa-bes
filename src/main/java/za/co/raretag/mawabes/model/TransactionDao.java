package za.co.raretag.mawabes.model;

import za.co.raretag.mawabes.entity.TransactionEntity;

public interface TransactionDao {
    String create(TransactionEntity transactionEntity);
    String update(TransactionEntity transactionEntity);
    String delete(String id);
    TransactionEntity findById(String id);

}
