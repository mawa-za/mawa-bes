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
    public String create(TransactionDto transactionDto) {
        try {
            TransactionEntity transactionEntity = new TransactionEntity(transactionDto);
            String id = numberRangeService.generateNumber(transactionEntity.getType());
            transactionEntity.setId(id);
            if (transactionDto.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setType(transactionDto.getType());
            transactionEntity.setSubType(transactionDto.getSubType());
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionEntity.setCreatedBy(userService.getCurrentUser());
            TransactionEntity createdTransactionEntity = transactionRepository.save(transactionEntity);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(id);
            creationDate.setType(DateType.CREATED);
            addDate(creationDate);
            return createdTransactionEntity.getId();
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
    public ArrayList<TransactionDateDto> getDates(String id) {
        ArrayList<TransactionDateDto> transactionDateDtos = new ArrayList<>();
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
    public ArrayList<TransactionAttachmentDto> getAttachments(String id) {
        return null;
    }

    @Override
    public void addLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public void removeLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public ArrayList<TransactionLinkDto> getLinks(String id) {
        return null;
    }

    @Override
    public ArrayList<TransactionPartnerDto> getPartners(String transactionId) {
        ArrayList<TransactionPartnerDto> transactionPartnerDtos = new ArrayList<>();
        List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findPartnerByTransaction(transactionId);
        for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto(transactionPartnerEntity);
            transactionPartnerDtos.add(transactionPartnerDto);
        }
        return transactionPartnerDtos;
    }

    @Override
    public ArrayList<TransactionDto> search(TransactionQueryDto query) {
        ArrayList<TransactionDto> transactionDtos = new ArrayList<>();
        if (query.getPartnerFunction() != null && query.getPartnerNo() != null) {
            List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findTransactionByPartner(query.getPartnerNo());
            for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
                if (transactionPartnerEntity.getTransactionPartnerPK().getPartnerFunction().equals(query.getPartnerFunction())) {
                    TransactionDto object = getTransaction(transactionPartnerEntity.getTransactionPartnerPK().getTransaction());
                    transactionDtos.add(object);
                }
            }
        }

        if (query.getType() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByType(query.getType());
            for (TransactionEntity transactionEntity : transactions) {
                TransactionDto transactionDto = getTransaction(transactionEntity.getId());
                transactionDtos.add(transactionDto);
            }
        }

        if (query.getStatus() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByStatus(query.getStatus());
            for (TransactionEntity transactionEntity : transactions) {
                TransactionDto transactionDto = getTransaction(transactionEntity.getId());
                transactionDtos.add(transactionDto);
            }
        }

        return transactionDtos;
    }

    @Override
    public void edit(TransactionDto transactionDto) {

    }

    @Override
    public TransactionDto getTransaction(String orderId) {
        TransactionDto transactionDto = null;
        TransactionEntity transactionEntity = transactionRepository.getById(orderId);
        if (transactionEntity != null) {
            transactionDto = new TransactionDto();
            transactionDto.setId(transactionEntity.getId());
            if (transactionEntity.getSubType() != null) {
                transactionDto.setSubType(transactionEntity.getSubType());
            }
            if (transactionEntity.getLocation() != null) {
                transactionDto.setLocation(transactionEntity.getLocation());
            }
            if (transactionEntity.getType() != null) {
                transactionDto.setType(transactionEntity.getType());
            }
            transactionDto.setStatus(StringConversion.capitalizeFully(transactionEntity.getStatus().replaceAll("_", " ")));
            if (transactionEntity.getStatusReason() != null) {
                transactionDto.setStatusReason(StringConversion.capitalizeFully(transactionEntity.getStatusReason().replaceAll("_", " ")));
            }
            if (transactionEntity.getSubStatus() != null) {
                transactionDto.setSubStatus(StringConversion.capitalizeFully(transactionEntity.getSubStatus()));
            }
            if (transactionEntity.getDescription() != null) {
                transactionDto.setDescription(transactionEntity.getDescription());
            }
            if (transactionEntity.getSubDescription() != null) {
                transactionDto.setSubDescription(transactionEntity.getSubDescription());
            }
            if (transactionEntity.getValidTo() != null) {
                transactionDto.setValidTo(Conversion.dateToString(transactionEntity.getValidTo()));
            }
            if (transactionEntity.getValidFrom() != null) {
                transactionDto.setValidFrom(Conversion.dateToString(transactionEntity.getValidFrom()));
            }
            if (transactionEntity.getCreatedBy() != null) {
                transactionDto.setCreatedBy(transactionEntity.getCreatedBy());
            }
            if (transactionEntity.getChangedBy() != null) {
                transactionDto.setChangedBy(transactionEntity.getChangedBy());
            }
        }
        return transactionDto;
    }

    @Override
    public void addItem(TransactionItemDto transactionItemDto) {
        try {
            TransactionItemPKEntity transactionItemPKEntity = new TransactionItemPKEntity();
            transactionItemPKEntity.setTransaction(transactionItemDto.getTransaction());
            String itemUUID = UUID.randomUUID().toString().replace("-", "");
            transactionItemPKEntity.setItem(itemUUID);

            TransactionItemEntity transactionItemEntity = new TransactionItemEntity();
            transactionItemEntity.setTransactionItemPKEntity(transactionItemPKEntity);
            transactionItemEntity.setProduct(transactionItemDto.getProduct());
            transactionItemEntity.setUnitOfMeasure(transactionItemDto.getUnitOfMeasure());
            transactionItemEntity.setUnitPrice(transactionItemDto.getUnitPrice());
            transactionItemEntity.setQuantity(transactionItemDto.getQuantity());
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
    public ArrayList<TransactionItemDto> getItems(String id) {
        ArrayList<TransactionItemDto> transactionItemDtos = new ArrayList<>();
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
    public ArrayList<TransactionAmountDto> getAmounts(String id) {
        ArrayList<TransactionAmountDto> transactionAmountDtos = new ArrayList<>();
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
            transactionPartnerPKEntity.setPartnerFunction(transactionPartnerDto.getFunction());
            transactionPartnerPKEntity.setPartnerNo(transactionPartnerDto.getPartner());

            TransactionPartnerEntity transactionPartnerEntity = new TransactionPartnerEntity();
            transactionPartnerEntity.setTransactionPartnerPK(transactionPartnerPKEntity);
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
        transactionPartnerPKEntity.setPartnerFunction(transactionPartnerDto.getFunction());
        transactionPartnerPKEntity.setPartnerNo(transactionPartnerDto.getPartner());
        try {
            transactionPartnerRepository.deleteById(transactionPartnerPKEntity);
        } catch (Exception ex) {
            throw new Exception("");
        }
    }

}
