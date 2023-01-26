package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.ArrayList;

public interface TransactionDao {
    String create(TransactionDto transactionDto);
    ArrayList<TransactionDto> search(TransactionQueryDto query);
    String edit(TransactionDto transactionDto);
    String delete(String id);
    TransactionDto getTransaction(String orderId);

    //Items
    ArrayList<MessageDto> addItem(TransactionItemDto transactionItemDto);
    ArrayList<MessageDto> removeItem(TransactionItemDto transactionItemDto);
    ArrayList<TransactionItemDto> getItems(String id);

    //Amounts
    ArrayList<MessageDto> addAmount(TransactionAmountDto transactionAmountDto);
    ArrayList<MessageDto> removeAmount(TransactionAmountDto transactionAmountDto);
    ArrayList<TransactionAmountDto> getAmounts(String id);

    //Partners
    ArrayList<MessageDto> addPartner(TransactionPartnerDto transactionPartnerDto);
    ArrayList<MessageDto> removePartner(TransactionPartnerDto transactionPartnerDto);
    ArrayList<TransactionPartnerDto> getPartners(String id);

    //Dates
    ArrayList<MessageDto> addDate(TransactionDateDto transactionDateDto);
    ArrayList<MessageDto> removeDate(TransactionDateDto transactionDateDto);
    ArrayList<TransactionDateDto> getDates(String id);


}
