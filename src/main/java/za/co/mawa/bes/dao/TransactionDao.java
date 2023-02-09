package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.ArrayList;
import java.util.List;

public interface TransactionDao {
    TransactionDto create(TransactionCreateDto transactionCreateDto);
    List<TransactionDto> search(TransactionQueryDto query);
    void edit(TransactionDto transactionDto);
    void delete(String id);
    TransactionDto get(String orderId);

    //Items
    void addItem(TransactionItemDto transactionItemDto) throws Exception;
    void removeItem(TransactionItemDto transactionItemDto) throws Exception;
    List<TransactionItemDto> getItems(String id);

    //Amounts
    void addAmount(TransactionAmountDto transactionAmountDto);
    void removeAmount(TransactionAmountDto transactionAmountDto) throws Exception;
    List<TransactionAmountDto> getAmounts(String id);

    //Partners
    void addPartner(TransactionPartnerDto transactionPartnerDto) throws Exception;
    void removePartner(TransactionPartnerDto transactionPartnerDto) throws Exception;
    List<TransactionPartnerDto> getPartners(String id);

    //Dates
    void addDate(TransactionDateDto transactionDateDto) throws Exception;
    void removeDate(TransactionDateDto transactionDateDto) throws Exception;
    List<TransactionDateDto> getDates(String id);

    //Attachments
    void addAttachment(TransactionAttachmentDto transactionAttachmentDto);
    void removeAttachment(TransactionAttachmentDto transactionAttachmentDto);
    List<TransactionAttachmentDto> getAttachments(String id);

    void addLink(TransactionLinkDto transactionLinkDto);
    void removeLink(TransactionLinkDto transactionLinkDto);
    List<TransactionLinkDto> getLinks(String id);
}
