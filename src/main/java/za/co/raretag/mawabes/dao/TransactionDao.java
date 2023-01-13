package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.MessageDto;
import za.co.raretag.mawabes.dto.OrderDateDto;
import za.co.raretag.mawabes.entity.TransactionEntity;

import java.util.ArrayList;

public interface TransactionDao {
    String create(TransactionEntity transactionEntity);
    String update(TransactionEntity transactionEntity);
    String delete(String id);
    TransactionEntity findById(String id);
    ArrayList<MessageDto> addDate(OrderDateDto od);

}
