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
    void addItem(TransactionItemDto transactionItemDto);
    void removeItem(TransactionItemDto transactionItemDto);
    ArrayList<TransactionItemDto> getItems(String id);

    //Amounts
    void addAmount(TransactionAmountDto transactionAmountDto);
    void removeAmount(TransactionAmountDto transactionAmountDto);
    ArrayList<TransactionAmountDto> getAmounts(String id);

    //Partners
    void addPartner(TransactionPartnerDto transactionPartnerDto);
    void removePartner(TransactionPartnerDto transactionPartnerDto);
    ArrayList<TransactionPartnerDto> getPartners(String id);

    //Dates
    void addDate(TransactionDateDto transactionDateDto);
    void removeDate(TransactionDateDto transactionDateDto);
    ArrayList<TransactionDateDto> getDates(String id);

    //Attachments
    void addAttachment(TransactionAttachmentDto transactionAttachmentDto);
    void removeDate(TransactionAttachmentDto transactionAttachmentDto);
    ArrayList<TransactionAttachmentDto> getAttachments(String id);

    void addLink(TransactionLinkDto transactionLinkDto);
    void removeLink(TransactionLinkDto transactionLinkDto);
    ArrayList<TransactionLinkDto> getLinks(String id);
}
