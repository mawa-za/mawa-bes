package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
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
    UserService userService;

    @Override
    public TransactionDto create(TransactionCreateDto transactionCreateDto) {
        try {
            TransactionEntity transactionEntity = new TransactionEntity(transactionCreateDto);
            String id = numberRangeService.generateNumber(transactionEntity.getType());
            transactionEntity.setNumber(id);
            if (transactionCreateDto.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setType(transactionCreateDto.getType());
            transactionEntity.setSubType(transactionCreateDto.getSubType());
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionEntity.setCreatedBy(userService.getCurrentUser());
            TransactionEntity createdTransactionEntity = transactionRepository.save(transactionEntity);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(createdTransactionEntity.getId());
            creationDate.setType(DateType.CREATED);
            addDate(creationDate);

            if (transactionCreateDto.getCustomer() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(createdTransactionEntity.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(transactionCreateDto.getCustomer());
                addPartner(transactionPartnerDto);
            }
            if (transactionCreateDto.getSupplier() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(createdTransactionEntity.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SUPPLIER);
                transactionPartnerDto.setPartner(transactionCreateDto.getCustomer());
                addPartner(transactionPartnerDto);
            }
            if (transactionCreateDto.getClaimant() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(createdTransactionEntity.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CLAIMANT);
                transactionPartnerDto.setPartner(transactionCreateDto.getCustomer());
                addPartner(transactionPartnerDto);
            }
            return new TransactionDto(createdTransactionEntity);
        } catch (NumberRangeObjectNotFound ex) {
            throw new RuntimeException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void delete(String id) {

    }

    @Override
    public void addDate(TransactionDateDto transactionDateDto) throws Exception {
        TransactionDateEntity transactionDateEntity = new TransactionDateEntity(transactionDateDto);
        if (transactionDateDto.getValue() != null) {
            transactionDateEntity.setValue(transactionDateDto.getValue());
        } else {
            transactionDateEntity.setValue(new Date());
        }
        try {
            transactionDateRepository.save(transactionDateEntity);
        } catch (Exception ex) {
            throw new Exception("Date not added");
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
    public void addAttachment(TransactionAttachmentDto transactionAttachmentDto) {

    }

    @Override
    public void removeAttachment(TransactionAttachmentDto transactionAttachmentDto) {

    }

    @Override
    public List<TransactionAttachmentDto> getAttachments(String id) {
        return null;
    }

    @Override
    public void addLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public void removeLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public List<TransactionLinkDto> getLinks(String id) {
        return null;
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
    public List<TransactionDto> search(TransactionQueryDto transactionQueryDto) {
        List<TransactionDto> transactionDtos = new ArrayList<>();
        if (transactionQueryDto.getPartnerFunction() != null && transactionQueryDto.getPartnerNo() != null) {
            List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findTransactionByPartner(transactionQueryDto.getPartnerNo());
            for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
                if (transactionPartnerEntity.getTransactionPartnerPKEntity().getFunction().equals(transactionQueryDto.getPartnerFunction())) {
                    TransactionDto object = get(transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction());
                    transactionDtos.add(object);
                }
            }
        }

        if (transactionQueryDto.getType() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByType(transactionQueryDto.getType());
            for (TransactionEntity transactionEntity : transactions) {
                TransactionDto transactionDto = get(transactionEntity.getId());
                transactionDtos.add(transactionDto);
            }
        }

        if (transactionQueryDto.getStatus() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByStatus(transactionQueryDto.getStatus());
            for (TransactionEntity transactionEntity : transactions) {
                TransactionDto transactionDto = get(transactionEntity.getId());
                transactionDtos.add(transactionDto);
            }
        }
        return transactionDtos;
    }

    @Override
    public void edit(TransactionDto transactionDto) {

    }

    @Override
    public TransactionDto get(String orderId) {
        TransactionDto transactionDto = null;
        TransactionEntity transactionEntity = transactionRepository.getById(orderId);
        if (transactionEntity != null) {
            transactionDto = new TransactionDto(transactionEntity);
        }
        return transactionDto;
    }

    @Override
    public void addItem(TransactionItemDto transactionItemDto) {
        try {
            TransactionItemEntity transactionItemEntity = new TransactionItemEntity(transactionItemDto);
            String itemUUID = UUID.randomUUID().toString().replace("-", "");
            transactionItemEntity.getTransactionItemPKEntity().setItem(itemUUID);
            transactionItemEntity.setValidFrom(new Date());
            transactionItemEntity.setValidTo(new Date(Constant.END_DATE));
            transactionItemRepository.save(transactionItemEntity);
        } catch (Exception exception) {

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

    @Override
    public void addAmount(TransactionAmountDto transactionAmountDto) {
        try {
            TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
            transactionAmountPKEntity.setTransaction(transactionAmountDto.getTransaction());
            transactionAmountPKEntity.setType(transactionAmountDto.getType());

            TransactionAmountEntity transactionAmountEntity = new TransactionAmountEntity();
            transactionAmountEntity.setTransactionAmountPKEntity(transactionAmountPKEntity);
            transactionAmountEntity.setAmount(transactionAmountDto.getAmount());
            transactionAmountRepository.save(transactionAmountEntity);
        } catch (Exception exception) {

        }
    }

    @Override
    public void removeAmount(TransactionAmountDto transactionAmountDto) throws Exception {
        TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
        transactionAmountPKEntity.setTransaction(transactionAmountDto.getTransaction());
        transactionAmountPKEntity.setType(transactionAmountDto.getType());
        try {
            transactionAmountRepository.deleteById(transactionAmountPKEntity);
        } catch (Exception ex) {
            throw new Exception("");
        }
    }

    @Override
    public List<TransactionAmountDto> getAmounts(String id) {
        List<TransactionAmountDto> transactionAmountDtos = new ArrayList<>();
        List<TransactionAmountEntity> transactionAmountEntities = transactionAmountRepository.getTransactionAmounts(id);
        for (TransactionAmountEntity transactionAmountEntity : transactionAmountEntities) {
            transactionAmountDtos.add(new TransactionAmountDto(transactionAmountEntity));
        }
        return transactionAmountDtos;
    }

    @Override
    public void addPartner(TransactionPartnerDto transactionPartnerDto) {
        try {
            TransactionPartnerPKEntity transactionPartnerPKEntity = new TransactionPartnerPKEntity();
            transactionPartnerPKEntity.setTransaction(transactionPartnerDto.getTransaction());
            transactionPartnerPKEntity.setFunction(transactionPartnerDto.getFunction());
            transactionPartnerPKEntity.setPartner(transactionPartnerDto.getPartner());

            TransactionPartnerEntity transactionPartnerEntity = new TransactionPartnerEntity();
            transactionPartnerEntity.setTransactionPartnerPKEntity(transactionPartnerPKEntity);
            transactionPartnerEntity.setValidFrom(new Date());
            transactionPartnerEntity.setCreatedBy(userService.getCurrentUser());
            transactionPartnerEntity.setStatus(transactionPartnerDto.getStatus());

            transactionPartnerRepository.save(transactionPartnerEntity);
        } catch (Exception exception) {

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
            throw new Exception("");
        }
    }

}
