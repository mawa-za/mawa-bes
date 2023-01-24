package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.TransactionEntity;
import za.co.mawa.bes.object.transaction.TransactionDTO;

import java.util.ArrayList;

public interface TransactionDao {
    String create(TransactionEntity transactionEntity);
    String update(TransactionEntity transactionEntity);
    String delete(String id);
    TransactionEntity findById(String id);
    ArrayList<MessageDto> addDate(OrderDateDto od);
    ArrayList<OrderPartnerDto> getPartners (String transactionId);
    ArrayList<OrderHeaderDto> getTransactionByApprover(String approver);
    ArrayList<TransactionDTO> search(TransactionQueryDto query);
    OrderHeaderDto getHeader(String orderId);
}
