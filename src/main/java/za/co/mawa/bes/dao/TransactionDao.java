package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemEditDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionAmountPKEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.TransactionNotFound;

import java.util.List;
import java.util.Optional;

public interface TransactionDao {
    TransactionDto create(TransactionCreateDto transactionCreateDto);
    List<TransactionQueryResultDto> search(TransactionQueryDto query);
    boolean edit(TransactionEdit transactionEditDto) throws DoesNotExist, Exception;
    void delete(String id) throws Exception;
    TransactionDto get(String transactionId) throws TransactionNotFound;

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
    TransactionPartnerDto getPartner(String transaction, String partnerFunction);

    //Dates
    void addDate(TransactionDateDto transactionDateDto) throws Exception;
    void removeDate(TransactionDateDto transactionDateDto) throws Exception;
    List<TransactionDateDto> getDates(String id);

    //Attachments
    void addAttachment(TransactionAttachmentDto transactionAttachmentDto);
    void removeAttachment(TransactionAttachmentDto transactionAttachmentDto);
    List<TransactionAttachmentDto> getAttachments(String id);

    void addLink(TransactionLinkDto transactionLinkDto) throws Exception;
    void removeLink(TransactionLinkDto transactionLinkDto);
    List<TransactionLinkDto> getLinks(String id);
    TransactionAmountDto getAmount(TransactionAmountPKEntity id);
    TransactionLinkEntity getTransaction(String type,String transaction1);
    boolean partnerEdit(TransactionPartnerEdit transaction) throws DoesNotExist, Exception;
    boolean dateEdit(TransactionDateEdit transaction) throws DoesNotExist, Exception;
    boolean editAmount() throws DoesNotExist,Exception;
    boolean editItem(TransactionItemEditDto transactionItemEditDto) throws DoesNotExist,Exception;
}
