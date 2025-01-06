package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.premium.PremiumSearchDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountEditDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemEditDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.entity.transaction.*;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.dao.TransactionDao;

import java.util.*;

@Service
public class TransactionService implements TransactionDao {
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionItemRepository transactionItemRepository;
    @Autowired
    TransactionDateRepository transactionDateRepository;
    @Autowired
    TransactionPartnerRepository transactionPartnerRepository;
    @Autowired
    TransactionAmountRepository transactionAmountRepository;
    @Autowired
    TransactionAttachmentRepository transactionAttachmentRepository;
    @Autowired
    UserService userService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    ProductService productService;
    @Autowired
    TransactionLinkRepository transactionLinkRepository;
    @Autowired
    PartnerService partnerService;
    @Autowired
    TransactionBankAccountRepository transactionBankAccountRepository;
    @Autowired
    TransactionViewRepository transactionViewRepository;
    @Autowired
    PremiumRepository premiumRepository;

    @Override
    public TransactionDto create(TransactionCreateDto transactionCreateDto) {
        try {
            TransactionEntity transactionEntity = new TransactionEntity(transactionCreateDto);
            String id = numberRangeService.generateNumber(transactionEntity.getType());
            transactionEntity.setNumber(id);
            if (transactionCreateDto.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            } else {
                transactionEntity.setStatus(transactionCreateDto.getStatus());
            }
            transactionEntity.setStatusReason(transactionCreateDto.getStatusReason());
            transactionEntity.setType(transactionCreateDto.getType());
            transactionEntity.setSubType(transactionCreateDto.getSubType());
            transactionEntity.setCategory(transactionCreateDto.getCategory());
            transactionEntity.setPriority(transactionCreateDto.getPriority());
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionEntity.setCreatedBy(getUser());
            transactionEntity.setLocation(transactionCreateDto.getLocation());
            transactionEntity.setDescription(transactionCreateDto.getDescription());
            transactionEntity.setSubDescription(transactionCreateDto.getSubDescription());
            TransactionEntity createdTransactionEntity = transactionRepository.save(transactionEntity);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(createdTransactionEntity.getId());
            creationDate.setType(DateType.CREATED);
            creationDate.setValue(new Date());
            addDate(creationDate);

            if (transactionCreateDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(createdTransactionEntity.getId());
                transactionPartnerDto.setPartner(transactionCreateDto.getCustomerId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                addPartner(transactionPartnerDto);
            }
            if (transactionCreateDto.getSupplierId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(createdTransactionEntity.getId());
                transactionPartnerDto.setPartner(transactionCreateDto.getSupplierId());
                transactionPartnerDto.setFunction(PartnerFunction.SUPPLIER);
                addPartner(transactionPartnerDto);
            }

            if (transactionCreateDto.getEmployeeResponsible() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(createdTransactionEntity.getId());
                transactionPartnerDto.setPartner(transactionCreateDto.getEmployeeResponsible());
                transactionPartnerDto.setFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
                addPartner(transactionPartnerDto);
            }

            return new TransactionDto(createdTransactionEntity);
        } catch (NumberRangeObjectNotFound ex) {
            throw new RuntimeException("Object number range not found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void edit(TransactionEditDto transactionEditDto) throws Exception {
        TransactionEntity entity = transactionRepository.getById(transactionEditDto.getId());
        if (entity != null) {
            try {
                if (transactionEditDto.getStatus() != null) {
                    entity.setStatus(transactionEditDto.getStatus());
                }
                if (transactionEditDto.getStatusReason() != null) {
                    entity.setStatusReason(transactionEditDto.getStatusReason());
                }
                if (transactionEditDto.getDescription() != null) {
                    if (transactionEditDto.getDescription().length() > 255) {
                        entity.setDescription(transactionEditDto.getDescription());
                    } else if (transactionEditDto.getDescription().length() <= 255) {
                        entity.setSubDescription(transactionEditDto.getDescription());
                    }
                }
                entity.setChangedBy(getUser());
                transactionRepository.save(entity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new DoesNotExist();
        }
    }

    @Override
    public void delete(String id) throws Exception {
        try {
            transactionRepository.deleteById(id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addDate(TransactionDateDto transactionDateDto) throws TransactionDateAddException {
        TransactionDateEntity transactionDateEntity = new TransactionDateEntity(transactionDateDto);
        if (transactionDateDto.getValue() != null) {
            transactionDateEntity.setValue(transactionDateDto.getValue());
        } else {
            transactionDateEntity.setValue(new Date());
        }
        try {
            transactionDateRepository.save(transactionDateEntity);
        } catch (Exception ex) {
            throw new TransactionDateAddException("Error adding date to transaction");
        }
    }

    @Override
    public void removeDate(TransactionDateDto transactionDateDto) throws Exception {
        TransactionDatePKEntity transactionDatePKEntity = new TransactionDatePKEntity();
        transactionDatePKEntity.setTransaction(transactionDateDto.getTransaction());
        transactionDatePKEntity.setType(transactionDateDto.getType());
        try {
            transactionDateRepository.deleteById(transactionDatePKEntity);
        } catch (Exception ex) {
            throw new Exception("Date not added");
        }
    }

    @Override
    public List<TransactionDateDto> getDates(String id) {
        List<TransactionDateDto> transactionDateDtos = new ArrayList<>();
        List<TransactionDateEntity> transactionDateEntities = transactionDateRepository.getTransactionDates(id);
        for (TransactionDateEntity transactionDateEntity : transactionDateEntities) {
            transactionDateDtos.add(new TransactionDateDto(transactionDateEntity));
        }
        return transactionDateDtos;
    }

    @Override
    public boolean addAttachment(TransactionAttachmentEntity entity) throws Exception {
        try {
            transactionAttachmentRepository.save(entity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean removeAttachment(TransactionAttachmentPKEntity transactionAttachmentDto) {
        try {
            transactionAttachmentRepository.deleteById(transactionAttachmentDto);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ArrayList<TransactionAttachmentDto> getAttachments(String id) throws Exception {
        try {
            ArrayList<TransactionAttachmentDto> attachments = new ArrayList<>();
            for (TransactionAttachmentEntity attachment : transactionAttachmentRepository.findByTransaction(id)) {
                TransactionAttachmentDto attachmentDto = new TransactionAttachmentDto();
                attachmentDto.setTransaction(attachment.getTransactionAttachmentPKEntity().getTransaction());
                attachmentDto.setDocumentType(attachment.getTransactionAttachmentPKEntity().getDocumentType());
                attachmentDto.setFileId(attachment.getFileId());
                attachmentDto.setStatus(attachment.getStatus());
                attachmentDto.setValidFrom(Conversion.dateToString(attachment.getValidFrom()));
                attachmentDto.setValidTo(Conversion.dateToString(attachment.getValidTo()));
                attachments.add(attachmentDto);
            }
            return attachments;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addLink(TransactionLinkDto transactionLinkDto) throws Exception {

        TransactionLinkEntity transactionLinkEntity = new TransactionLinkEntity(transactionLinkDto);
        if (transactionLinkDto.getCreationDate() != null) {
            transactionLinkEntity.setCreation_date(transactionLinkDto.getCreationDate());
        } else {
            transactionLinkEntity.setCreation_date(new Date());
        }
        try {
            transactionLinkRepository.save(transactionLinkEntity);
        } catch (Exception ex) {
            throw new Exception("Error adding transaction link");
        }

    }

    @Override
    public void removeLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public List<TransactionLinkDto> getLinks(String id) {
        List<TransactionLinkDto> transactionLinkDtos = new ArrayList<>();
        List<TransactionLinkEntity> transactionLinkEntities = transactionLinkRepository.getTransactionLinks(id);
        for (TransactionLinkEntity transactionLinkEntity : transactionLinkEntities) {
            transactionLinkDtos.add(new TransactionLinkDto(transactionLinkEntity));
        }
        return transactionLinkDtos;

//        return null;
    }


    @Override
    public TransactionLinkEntity getTransaction(String type, String transaction1) {
        TransactionLinkEntity transaction = transactionLinkRepository.getTransactionLinks(transaction1, type);
        if (transaction != null) {
            return transaction;
        } else {
            return null;
        }

    }

    @Override
    public boolean partnerEdit(TransactionPartnerEdit transaction) throws DoesNotExist, Exception {
        TransactionPartnerEntity entity = transactionPartnerRepository.findPartnerByTransactionAndType(transaction.getTransaction(), transaction.getPartnerFunction());
        if (entity != null) {
            try {
                if (transaction.getParnter() != null) {
                    TransactionPartnerDto partnerRemove = new TransactionPartnerDto();
                    partnerRemove.setTransaction(entity.getTransactionPartnerPKEntity().getTransaction());
                    partnerRemove.setPartner(entity.getTransactionPartnerPKEntity().getPartner());
                    partnerRemove.setFunction(entity.getTransactionPartnerPKEntity().getFunction());
                    removePartner(partnerRemove);

                    TransactionPartnerDto partnerAdd = new TransactionPartnerDto();
                    partnerAdd.setTransaction(transaction.getTransaction());
                    partnerAdd.setPartner(transaction.getParnter());
                    partnerAdd.setFunction(transaction.getPartnerFunction());
                    addPartner(partnerAdd);
                }
                //entity.setChangedBy(getUser());
                //transactionPartnerRepository.save(entity);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                TransactionDto dto = get(transaction.getTransaction());
                if (dto != null) {
                    TransactionPartnerPKEntity pk = new TransactionPartnerPKEntity();
                    pk.setTransaction(transaction.getTransaction());
                    pk.setFunction(transaction.getPartnerFunction());
                    pk.setPartner(transaction.getParnter());
                    TransactionPartnerEntity entityParnter = new TransactionPartnerEntity();
                    entityParnter.setCreatedBy(getUser());
                    entityParnter.setTransactionPartnerPKEntity(pk);
                    transactionPartnerRepository.save(entityParnter);
                    return true;
                } else {
                    throw new DoesNotExist();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean dateEdit(TransactionDateEdit transaction) throws DoesNotExist, Exception {
        TransactionDateEntity entity = transactionDateRepository.getTransactionDatesType(transaction.getTransaction(), transaction.getType());
        if (entity != null) {
            try {
                if (transaction.getValue() != null) {
                    entity.setValue(transaction.getValue());
                }
                transactionDateRepository.save(entity);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                TransactionDto dto = get(transaction.getTransaction());
                if (dto != null) {
                    TransactionDatePKEntity pk = new TransactionDatePKEntity();
                    TransactionDateEntity entityCreate = new TransactionDateEntity();
                    pk.setTransaction(transaction.getTransaction());
                    pk.setType(transaction.getType());
                    entityCreate.setValue(transaction.getValue());
                    entityCreate.setTransactionDatePKEntity(pk);
                    transactionDateRepository.save(entityCreate);
                    return true;
                } else {
                    throw new DoesNotExist();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            //throw new DoesNotExist();
        }
    }

    @Override
    public boolean editDate(TransactionDateDto transactionDateDto) throws DoesNotExist, Exception {
        return false;
    }

    public boolean editAmount(TransactionAmountEditDto transactionAmountEditDto) throws DoesNotExist, Exception {
        try {
            TransactionAmountEntity entity = transactionAmountRepository.getById(transactionAmountEditDto.getId());
            if (entity != null) {
                entity.setAmount(transactionAmountEditDto.getAmount());
                transactionAmountRepository.save(entity);
                return true;
            } else {
                throw new DoesNotExist();
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public boolean editItem(TransactionItemEditDto item) throws DoesNotExist, Exception {
        boolean edited = false;
        if (item.getPreviousProduct() != null && item.getPreviousProduct() != "") {
            TransactionItemEntity transactionItem = transactionItemRepository.getTransactionItem(item.getTransaction(), item.getPreviousProduct());
            if (transactionItem != null) {
                try {
                    transactionItem.setValidTo(new Date());
                    transactionItemRepository.save(transactionItem);
                    ProductDto productDto = productService.get(item.getProduct());
                    TransactionItemDto transactionItemDto = new TransactionItemDto();
                    transactionItemDto.setTransaction(item.getTransaction());
                    transactionItemDto.setProduct(productDto.getId());
                    transactionItemDto.setUnitPrice(item.getUnitPrice());
                    transactionItemDto.setQuantity(item.getQuantity());
                    transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
                    addItem(transactionItemDto);
                    edited = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        } else {
            TransactionItemEntity transactionItem = transactionItemRepository.getTransactionItem(item.getTransaction(), item.getProduct());
            if (transactionItem != null) {
                transactionItem.setUnitPrice(item.getUnitPrice());
                transactionItemRepository.save(transactionItem);
                edited = true;

            }

        }
        return edited;
    }

    @Override
    public void addBankAccount(TransactionAccountDto accountDto) throws Exception {

        try {
            TransactionBankAccount entity = new TransactionBankAccount();
            entity.setTransaction(accountDto.getTransaction());
            entity.setBankName(accountDto.getBankName());
            entity.setAccountHolder(accountDto.getAccountHolder());
            entity.setAccountNumber(accountDto.getAccountNumber());
            entity.setAccountType(accountDto.getAccountType());
            entity.setBranchCode(accountDto.getBranchCode());
            transactionBankAccountRepository.save(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public boolean editBankAccount(TransactionAccountDto accountDto) throws Exception {
        return false;
    }

    @Override
    public TransactionAccountDto getBankAccount(String id) {
        TransactionBankAccount entity = transactionBankAccountRepository.getBankAccount(id);
        if (entity != null) {
            TransactionAccountDto account = new TransactionAccountDto();
            account.setAccountHolder(entity.getAccountHolder());
            account.setAccountType(entity.getAccountType());
            account.setAccountNumber(entity.getAccountNumber());
            account.setBankName(entity.getBankName());
            account.setTransaction(entity.getTransaction());
            account.setBranchCode(entity.getBranchCode());

            return account;
        } else {
            return null;
        }

    }

    @Override
    public TransactionAccountDto getOptionalBankAccount(String id) {
        Optional<TransactionBankAccount> entity = transactionBankAccountRepository.findById(id);
        TransactionBankAccount bankAccount = entity.orElse(null);
        if (bankAccount != null) {
            TransactionAccountDto account = new TransactionAccountDto();
            account.setAccountHolder(bankAccount.getAccountHolder());
            account.setAccountType(bankAccount.getAccountType());
            account.setAccountNumber(bankAccount.getAccountNumber());
            account.setBankName(bankAccount.getBankName());
            account.setTransaction(bankAccount.getTransaction());
            account.setBranchCode(bankAccount.getBranchCode());
            return account;
        }
        return null;
    }

    @Override
    public boolean removePartner(String id, String partnerFunction, String partner) throws Exception {
        try {
            TransactionPartnerPKEntity entityPk = new TransactionPartnerPKEntity();
            entityPk.setFunction(partnerFunction);
            entityPk.setTransaction(id);
            entityPk.setPartner(partner);
            transactionPartnerRepository.deleteById(entityPk);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public boolean removeDate(String id, String type) throws Exception {
        try {
            TransactionDatePKEntity pkEntity = new TransactionDatePKEntity();
            pkEntity.setTransaction(id);
            pkEntity.setType(type);
            transactionDateRepository.deleteById(pkEntity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override

    public List<TransactionPartnerDto> getPartnersByFunction(String partnerFunction) throws Exception {

        List<TransactionPartnerEntity> transactionPartnerEntityList = transactionPartnerRepository.findPartnerByType(partnerFunction);
        List<TransactionPartnerDto> transactionPartnerDtos = new ArrayList<>();
        if (!transactionPartnerEntityList.isEmpty()) {
            for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntityList) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto(transactionPartnerEntity);
                transactionPartnerDtos.add(transactionPartnerDto);
            }
        }


        return transactionPartnerDtos;
    }

    @Override
    public List<TransactionItemDto> getItemsBy(TransactionItemDto transactionItemDto) throws Exception {
        List<TransactionItemDto> transactionItemDtos = new ArrayList<>();
        if (transactionItemDto.getProduct() != null) {
            List<TransactionItemEntity> transactionItemEntities = transactionItemRepository.findItemBy(transactionItemDto.getProduct());
            if (!transactionItemEntities.isEmpty()) {
                for (TransactionItemEntity transactionItemEntity : transactionItemEntities) {
                    transactionItemDtos.add(new TransactionItemDto(transactionItemEntity));
                }
            }


        }

        return transactionItemDtos;
    }

    public List<TransactionPartnerDto> getPartnerType(String partner, String type) {

        List<TransactionPartnerDto> transactionPartnerDtos = new ArrayList<>();
        List<TransactionPartnerEntity> transactionPartnerEntityList = transactionPartnerRepository.findPartnerByPartnerAndType(partner, type);

        if (!transactionPartnerEntityList.isEmpty()) {

            for (TransactionPartnerEntity partnerEntity : transactionPartnerEntityList) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto(partnerEntity);
                transactionPartnerDtos.add(transactionPartnerDto);
            }
        }
        return transactionPartnerDtos;

    }

    @Override
    public List<TransactionPartnerDto> getPartners(String transactionId) {
        List<TransactionPartnerDto> transactionPartnerDtos = new ArrayList<>();
        List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findPartnerByTransaction(transactionId);
        for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto(transactionPartnerEntity);
            transactionPartnerDtos.add(transactionPartnerDto);
        }
        return transactionPartnerDtos;
    }

    @Override
    public TransactionPartnerDto getPartner(String transaction, String partnerFunction) {
        TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
        List<TransactionPartnerDto> transactionPartnerDtoList = getPartners(transaction).stream()
                .filter(a -> Objects.equals(a.getFunction(), partnerFunction))
                .toList();
        if (transactionPartnerDtoList.iterator().hasNext()) {
            return transactionPartnerDtoList.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public List<String> search(TransactionQueryDto transactionQueryDto) {
        List<String> searchTerms = new ArrayList<>();
        Map<String, String> resultList = new HashMap<>();
        List<String> transactionListId = new ArrayList<>();
        List<String> finalList = new ArrayList<>();
        if (transactionQueryDto.getType() != null) {
            searchTerms.add("TYPE");
            List<TransactionEntity> transactions = transactionRepository.findTransactionByType(transactionQueryDto.getType());
            for (TransactionEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getId());
                resultList.put("TYPE-" + transactionEntity.getId(), transactionEntity.getId());
            }
        }

        if (transactionQueryDto.getPartnerFunction() != null && transactionQueryDto.getPartnerNo() != null) {
            searchTerms.add("EMPLOYEE-RESPONSIBLE");
            List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findTransactionByPartner(transactionQueryDto.getEmployeeResponsibleId());
            for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {

                transactionListId.add(transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction());
                resultList.put("EMPLOYEE-RESPONSIBLE-" + transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction(), transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction());

            }
        }

        if (transactionQueryDto.getPartnerFunction() != null && transactionQueryDto.getPartnerNo() != null) {
            searchTerms.add("PARTNER-FUNCTION");
            List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findTransactionByPartner(transactionQueryDto.getPartnerNo());
            for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
                if (transactionPartnerEntity.getTransactionPartnerPKEntity().getFunction().equals(transactionQueryDto.getPartnerFunction())) {
                    transactionListId.add(transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction());
                    resultList.put("PARTNER-FUNCTION-" + transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction(), transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction());
                }
            }
        }

        if (transactionQueryDto.getStatus() != null) {
            searchTerms.add("STATUS");
            List<TransactionEntity> transactions = transactionRepository.findTransactionByStatus(transactionQueryDto.getStatus());
            for (TransactionEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getId());
                resultList.put("STATUS-" + transactionEntity.getId(), transactionEntity.getId());
            }
        }
        if (transactionQueryDto.getChangedBy() != null && transactionQueryDto.getType() != null) {
            searchTerms.add("CHANGED-BY");
            List<TransactionEntity> transactions = transactionRepository.findTransactionByChangedBy(transactionQueryDto.getChangedBy(), transactionQueryDto.getType());
            for (TransactionEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getId());
                resultList.put("CHANGED-BY-" + transactionEntity.getId(), transactionEntity.getId());
            }
        }
        if (transactionQueryDto.getCreatedBy() != null && transactionQueryDto.getType() != null) {
            searchTerms.add("CREATED-BY");
            List<TransactionEntity> transactions = transactionRepository.findTransactionByCreatedBy(transactionQueryDto.getCreatedBy(), transactionQueryDto.getType());
            for (TransactionEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getId());
                resultList.put("CREATED-BY-" + transactionEntity.getId(), transactionEntity.getId());
            }
        }
        if (transactionQueryDto.getSubtype() != null) {
            searchTerms.add("SUB-TYPE");
            List<TransactionEntity> transactions = transactionRepository.findTransactionBySubType(transactionQueryDto.getSubtype());
            for (TransactionEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getId());
                resultList.put("SUB-TYPE-" + transactionEntity.getId(), transactionEntity.getId());
            }
        }
        if (transactionQueryDto.getNumber() != null) {
            searchTerms.add("NUMBER");
            List<TransactionEntity> transactions = transactionRepository.findTransactionByNumber(transactionQueryDto.getNumber());
            for (TransactionEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getId());
                resultList.put("NUMBER-" + transactionEntity.getId(), transactionEntity.getId());
            }
        }
        if (transactionQueryDto.getValue() != null && transactionQueryDto.getDateType() != null) {
            searchTerms.add("DATE");
            List<TransactionDateEntity> transactions = transactionDateRepository.findByDateType(transactionQueryDto.getValue(), transactionQueryDto.getDateType());
            for (TransactionDateEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getTransactionDatePKEntity().getTransaction());
                resultList.put("DATE-" + transactionEntity.getTransactionDatePKEntity().getTransaction(), transactionEntity.getTransactionDatePKEntity().getTransaction());
            }
        }
        if (transactionQueryDto.getTransactionlink() != null) {
            searchTerms.add("LINK");
            List<TransactionLinkEntity> transactions = transactionLinkRepository.getTransactionLinks(transactionQueryDto.getTransactionlink());
            for (TransactionLinkEntity transactionEntity : transactions) {
                transactionListId.add(transactionEntity.getTransactionLinkPKEntity().getTransaction2());
                resultList.put("LINK-" + transactionEntity.getTransactionLinkPKEntity().getTransaction2(), transactionEntity.getTransactionLinkPKEntity().getTransaction2());
            }
        }

        if (transactionQueryDto.getParent() != null) {
            searchTerms.add("CHILD");
            for (TransactionLinkEntity transactionLinkEntity: transactionLinkRepository.getChildren(transactionQueryDto.getParent())){
                transactionListId.add(transactionLinkEntity.getTransactionLinkPKEntity().getTransaction2());
                resultList.put("CHILD-" + transactionLinkEntity.getTransactionLinkPKEntity().getTransaction2(), transactionLinkEntity.getTransactionLinkPKEntity().getTransaction2());
            }
        }

        String searchStr = "";
        boolean found = false;
        for (String id : transactionListId) {
            for (String term : searchTerms) {
                String key = term + "-" + id;
                if (resultList.containsKey(key)) {
                    found = true;
                } else {
                    found = false;
                }
            }
            if (found) {
                if(!finalList.contains(id)){
                    finalList.add(id);
                }
            }
        }
        return finalList;
    }

    public List<TransactionViewEntity> searchV2(TransactionViewDto transactionViewDto) {
        List<TransactionViewEntity> transactionList = new ArrayList<>();

        for (TransactionViewEntity entity  : transactionViewRepository.findAllByType(transactionViewDto.getType())) {
            try {

                boolean match = true;

                if(transactionViewDto.getMainPartner() != null) {
                    String customerName = entity.getMainPartner().replace(" ", "");
                    match =  transactionViewDto.getMainPartner().replace(" ", "").equals(customerName);
                }

                if(transactionViewDto.getStatus() != null) {
                    String status = entity.getTransactionStatus();
                    match =    match &&  transactionViewDto.getStatus().equals(status);
                }
                if(transactionViewDto.getSubType() != null) {
                    String subType = entity.getTransactionSubtype();
                    match =    match &&  transactionViewDto.getSubType().equals(subType);
                }

                if(transactionViewDto.getIdNumber() != null) {
                    String IdNumber = entity.getIdentityNumber();
                    match =  match && transactionViewDto.getIdNumber().equals(IdNumber);
                }

                if(transactionViewDto.getEmployeeResponsibleName() !=null){
                    String employeeResponsibleName = entity.getEmployeeResponsible().replace(" ", "");
                    match = match && transactionViewDto.getEmployeeResponsibleName().replace(" ", "").equals(employeeResponsibleName);
                }

                if(transactionViewDto.getCreationDate() !=null){

                    String dateJoined = Conversion.dateToString(entity.getCreationDate());

                    String queryDateJoined = Conversion.dateToString(transactionViewDto.getCreationDate());

                    match = match && dateJoined.equals(queryDateJoined);


                }

                if(match) {
                    transactionList.add(entity);
                }

            }catch (Exception e){

            }
        }
        return transactionList;
    }

    @Override
    public TransactionDto get(String transactionId) throws TransactionNotFound {
        TransactionEntity transactionEntity = transactionRepository.getById(transactionId);
        if (transactionEntity != null) {
            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setId(transactionEntity.getId());
            transactionDto.setNumber(transactionEntity.getNumber());
            transactionDto.setDescription(transactionEntity.getDescription());
            transactionDto.setSubDescription(transactionEntity.getSubDescription());
            transactionDto.setType(transactionEntity.getType());
            transactionDto.setSubType(transactionEntity.getSubType());
            transactionDto.setCategory(transactionEntity.getCategory());
            transactionDto.setPriority(transactionEntity.getPriority());
            transactionDto.setLocation(transactionEntity.getLocation());
            transactionDto.setStatus(transactionEntity.getStatus());
            if (transactionEntity.getCreatedBy() != null) {
                transactionDto.setCreatedBy(transactionEntity.getCreatedBy());
            }
            if (transactionEntity.getChangedBy() != null) {
                transactionDto.setChangedBy(transactionEntity.getChangedBy());
            }
            if (transactionEntity.getStatusReason() != null) {
                transactionDto.setStatusReason(StringConversion.capitalizeFully(transactionEntity.getStatusReason().replaceAll("_", " ")));
            }

            for (TransactionPartnerDto transactionPartnerDto : getPartners(transactionId)) {

                if (transactionPartnerDto.getFunction().equals(PartnerFunction.CUSTOMER)) {
                    transactionDto.setCustomerId(transactionPartnerDto.getPartner());
                }
                if (transactionPartnerDto.getFunction().equals(PartnerFunction.SUPPLIER)) {
                    transactionDto.setSupplierId(transactionPartnerDto.getPartner());
                }
            }

            for (TransactionDateDto transactionDateDto : getDates(transactionId)) {
                if (transactionDateDto.getType().equals(DateType.ORDER_DATE)) {
                    transactionDto.setOrderDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.INVOICE_DATE)) {
                    transactionDto.setInvoiceDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.DELIVERY_DATE)) {
                    transactionDto.setDeliveryDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.EXPIRY_DATE)) {
                    transactionDto.setExpiryDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.DUE_DATE)) {
                    transactionDto.setDueDate(transactionDateDto.getValue());
                }
            }

            return transactionDto;
        } else {
            throw new TransactionNotFound("Transaction not found");
        }

    }

    @Override
    public void addItem(TransactionItemDto transactionItemDto) throws TransactionItemAddException {
        try {
            TransactionItemEntity transactionItemEntity = new TransactionItemEntity(transactionItemDto);
            String itemUUID = UUID.randomUUID().toString().replace("-", "");
            transactionItemEntity.getTransactionItemPKEntity().setItem(itemUUID);
            transactionItemEntity.setUnitPrice(transactionItemDto.getUnitPrice());
            transactionItemEntity.setQuantity(transactionItemDto.getQuantity());
            transactionItemEntity.setUnitOfMeasure(transactionItemDto.getBaseUnitOfMeasure());
            transactionItemEntity.setValidFrom(new Date());
            transactionItemEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionItemRepository.save(transactionItemEntity);
        } catch (Exception exception) {
            throw new TransactionItemAddException("Error adding item to transaction");
        }
    }

    @Override
    public void removeItem(TransactionItemDto transactionItemDto) throws Exception {
        TransactionItemPKEntity transactionItemPKEntity = new TransactionItemPKEntity();
        transactionItemPKEntity.setTransaction(transactionItemDto.getTransaction());
        transactionItemPKEntity.setItem(transactionItemDto.getItem());
        try {
            transactionItemRepository.deleteById(transactionItemPKEntity);
        } catch (Exception ex) {
            throw new Exception("");
        }
    }

    @Override
    public List<TransactionItemDto> getItems(String id) {
        List<TransactionItemDto> transactionItemDtos = new ArrayList<>();
        List<TransactionItemEntity> transactionItemEntities = transactionItemRepository.getTransactionItems(id);
        for (TransactionItemEntity transactionItemEntity : transactionItemEntities) {
            transactionItemDtos.add(new TransactionItemDto(transactionItemEntity));
        }
        return transactionItemDtos;
    }

    public void removeAmount(String id) throws Exception {
        try {
            transactionAmountRepository.deleteById(id);
        } catch (Exception ex) {
            throw new Exception("");
        }
    }


    public List<TransactionAmountDto> getAmounts(String id) {
        List<TransactionAmountDto> transactionAmountDtos = new ArrayList<>();
        List<TransactionAmountEntity> transactionAmountEntities = transactionAmountRepository.getByTransaction(id);
        for (TransactionAmountEntity transactionAmountEntity : transactionAmountEntities) {
//            transactionAmountDtos.add(new TransactionAmountDto(transactionAmountEntity));
        }
        return transactionAmountDtos;
    }

    @Override
    public void addPartner(TransactionPartnerDto transactionPartnerDto) throws TransactionPartnerAddException {
        try {
            TransactionPartnerPKEntity transactionPartnerPKEntity = new TransactionPartnerPKEntity();
            transactionPartnerPKEntity.setTransaction(transactionPartnerDto.getTransaction());
            transactionPartnerPKEntity.setFunction(transactionPartnerDto.getFunction());
            transactionPartnerPKEntity.setPartner(transactionPartnerDto.getPartner());
            TransactionPartnerEntity transactionPartnerEntity = new TransactionPartnerEntity();
            transactionPartnerEntity.setTransactionPartnerPKEntity(transactionPartnerPKEntity);
            transactionPartnerEntity.setValidFrom(new Date());
            transactionPartnerEntity.setCreatedBy(getUser());
            transactionPartnerEntity.setStatus(transactionPartnerDto.getStatus());
            if (transactionPartnerDto.getDateAdded() != null) {
                transactionPartnerEntity.setDateAdded(Conversion.stringToDate(transactionPartnerDto.getDateAdded()));
            }
            if (transactionPartnerDto.getDateEffective() != null) {
                transactionPartnerEntity.setDateEffective(Conversion.stringToDate(transactionPartnerDto.getDateEffective()));
            }
            transactionPartnerRepository.save(transactionPartnerEntity);
        } catch (Exception exception) {
            throw new TransactionPartnerAddException("Could not add partner to transaction");
        }
    }

    @Override
    public void removePartner(TransactionPartnerDto transactionPartnerDto) throws Exception {
        TransactionPartnerPKEntity transactionPartnerPKEntity = new TransactionPartnerPKEntity();
        transactionPartnerPKEntity.setTransaction(transactionPartnerDto.getTransaction());
        transactionPartnerPKEntity.setFunction(transactionPartnerDto.getFunction());
        transactionPartnerPKEntity.setPartner(transactionPartnerDto.getPartner());
        try {
            transactionPartnerRepository.deleteById(transactionPartnerPKEntity);
        } catch (Exception ex) {
            throw new Exception("Could not remove partner to transaction");
        }
    }

    private String getUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }

    public List<PremiumEntity> search(PremiumSearchDto premiumSearchDto) {
        List<PremiumEntity> premiumEntityList = premiumRepository.findAll();
        List<PremiumEntity> premiumEntities = new ArrayList<>();

        for (PremiumEntity premium : premiumEntityList) {
            try {
                boolean match = true;

                if(premiumSearchDto.getEmployeeResponsible() != null) {

                    match =  premium.getCreatedBy().equals(premiumSearchDto.getEmployeeResponsible());
                }

                if(premiumSearchDto.getTenderType() != null) {

                    match =  match && premium.getTenderType().equals(premiumSearchDto.getTenderType());
                }

                if(premiumSearchDto.getMembershipId() != null){
                    match = match && premium.getMembershipId().equals(premiumSearchDto.getMembershipId());
                }

                if(premiumSearchDto.getLocation() !=null){

                    match = match && premium.getLocation().equals(premiumSearchDto.getLocation());
                }

                if(match) {
                    premiumEntities.add(premium);
                }

            }catch (Exception e){

            }
        }
        return premiumEntities;
    }

}
