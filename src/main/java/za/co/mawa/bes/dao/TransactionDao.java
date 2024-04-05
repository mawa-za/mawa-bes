package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.date.TransactionDateEditDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemEditDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.*;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.exception.TransactionPartnerAddException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public interface TransactionDao {
    TransactionDto create(TransactionCreateDto transactionCreateDto);
    List<String> search(TransactionQueryDto query);
    void edit(TransactionEditDto transactionEditDto) throws DoesNotExist, Exception;
    void delete(String id) throws Exception;
    TransactionDto get(String transactionId) throws TransactionNotFound;

    //Items
    void addItem(TransactionItemDto transactionItemDto) throws Exception;
    void removeItem(TransactionItemDto transactionItemDto) throws Exception;
    List<TransactionItemDto> getItems(String id);
    //Partners
    void addPartner(TransactionPartnerDto transactionPartnerDto) throws TransactionPartnerAddException;
    void removePartner(TransactionPartnerDto transactionPartnerDto) throws Exception;
    List<TransactionPartnerDto> getPartners(String id);
    TransactionPartnerDto getPartner(String transaction, String partnerFunction);

    //Dates
    void addDate(TransactionDateDto transactionDateDto) throws Exception;
    void removeDate(TransactionDateDto transactionDateDto) throws Exception;
    List<TransactionDateDto> getDates(String id);

    //Attachments
    boolean addAttachment(TransactionAttachmentEntity entity) throws Exception;
    boolean removeAttachment(TransactionAttachmentPKEntity transactionAttachmentDto);
    ArrayList<TransactionAttachmentDto> getAttachments(String id) throws Exception;

    void addLink(TransactionLinkDto transactionLinkDto) throws Exception;
    void removeLink(TransactionLinkDto transactionLinkDto);
    List<TransactionLinkDto> getLinks(String id);
    TransactionLinkEntity getTransaction(String type,String transaction1);
    boolean partnerEdit(TransactionPartnerEdit transaction) throws DoesNotExist, Exception;
    boolean dateEdit(TransactionDateEdit transaction) throws DoesNotExist, Exception;
    boolean editDate(TransactionDateDto transactionDateDto) throws DoesNotExist, Exception;
    boolean editItem(TransactionItemEditDto transactionItemEditDto) throws DoesNotExist,Exception;
    void addBankAccount(TransactionAccountDto accountDto) throws Exception;
    boolean editBankAccount(TransactionAccountDto accountDto) throws Exception;
    TransactionAccountDto getBankAccount(String id);
    TransactionAccountDto getOptionalBankAccount(String id);
    boolean removePartner(String id,String partnerFunction,String partner) throws Exception;
    boolean removeDate(String id,String type) throws Exception;


    List<TransactionPartnerDto> getPartnersByFunction(String partnerFunction)  throws  Exception ;

    List<TransactionItemDto> getItemsBy(TransactionItemDto transactionItemDto) throws  Exception;

    List<TransactionPartnerDto> getPartnerType(String partner, String partnerFunction);

}
