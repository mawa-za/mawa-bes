package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.MessageDto;
import za.co.mawa.bes.dto.OrderDateDto;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.ArrayList;

public interface TransactionDao {
    String create(TransactionEntity transactionEntity);
    String update(TransactionEntity transactionEntity);
    String delete(String id);
    TransactionEntity findById(String id);
    ArrayList<MessageDto> addDate(OrderDateDto od);

}
