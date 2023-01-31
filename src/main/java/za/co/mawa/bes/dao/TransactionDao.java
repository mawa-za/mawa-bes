package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.ArrayList;

public interface TransactionDao {
    TransactionDto create(TransactionDto transactionDto);
    ArrayList<TransactionDto> search(TransactionQueryDto query);
    void edit(TransactionDto transactionDto);
    void delete(String id);
    TransactionDto getTransaction(String orderId);

    //Items
    void addItem(TransactionItemDto transactionItemDto);
    void removeItem(TransactionItemDto transactionItemDto) throws Exception;
    ArrayList<TransactionItemDto> getItems(String id);

    //Amounts
    void addAmount(TransactionAmountDto transactionAmountDto);
    void removeAmount(TransactionAmountDto transactionAmountDto) throws Exception;
    ArrayList<TransactionAmountDto> getAmounts(String id);

    //Partners
    void addPartner(TransactionPartnerDto transactionPartnerDto);
    void removePartner(TransactionPartnerDto transactionPartnerDto) throws Exception;
    ArrayList<TransactionPartnerDto> getPartners(String id);

    //Dates
    void addDate(TransactionDateDto transactionDateDto) throws Exception;
    void removeDate(TransactionDateDto transactionDateDto) throws Exception;
    ArrayList<TransactionDateDto> getDates(String id);

    //Attachments
    void addAttachment(TransactionAttachmentDto transactionAttachmentDto);
    void removeAttachment(TransactionAttachmentDto transactionAttachmentDto);
    ArrayList<TransactionAttachmentDto> getAttachments(String id);

    void addLink(TransactionLinkDto transactionLinkDto);
    void removeLink(TransactionLinkDto transactionLinkDto);
    ArrayList<TransactionLinkDto> getLinks(String id);
}
