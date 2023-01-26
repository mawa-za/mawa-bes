package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.TransactionPartnerEntity;
import za.co.mawa.bes.repository.TransactionDateRepository;
import za.co.mawa.bes.repository.TransactionPartnerRepository;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.dao.TransactionDao;
import za.co.mawa.bes.entity.TransactionDateEntity;
import za.co.mawa.bes.entity.TransactionDatePKEntity;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService implements TransactionDao {

    @Autowired
    NumberRangeService numberRange;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionDateRepository transactionDateRepository;
    @Autowired
    TransactionPartnerRepository transactionPartnerRepository;

    @Autowired
    JwtUserDetailsService jwtUserDetailsService;

    @Override
    public String create(TransactionDto transactionDto) {
        TransactionEntity transactionEntity = new TransactionEntity();
        String txnNo = null;
        try {
            txnNo = numberRange.generateNumber(transactionEntity.getType());
            transactionEntity.setId(txnNo);
            if (transactionDto.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setType(transactionDto.getType());
            transactionEntity.setSubType(transactionDto.getSubType());
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionEntity.setCreatedBy(UserContext.getCurrentUser());
            transactionRepository.save(transactionEntity);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(txnNo);
            creationDate.setType(DateType.CREATED);

            addDate(creationDate);
        } catch (Exception ex) {
            return null;
        }
        return txnNo;
    }

    @Override
    public String delete(String id) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> addDate(TransactionDateDto transactionDateDto) {
        TransactionDatePKEntity txnDatePK = new TransactionDatePKEntity();
        txnDatePK.setTransaction(transactionDateDto.getTransaction());
        txnDatePK.setType(transactionDateDto.getType());
        TransactionDateEntity txnDate = new TransactionDateEntity();
        txnDate.setTransactionDatePK(txnDatePK);
        if (transactionDateDto.getValue() != null) {
            txnDate.setValue(Conversion.stringToDate(transactionDateDto.getValue()));
        } else {
            txnDate.setValue(new Date());
        }
        try {
            transactionDateRepository.save(txnDate);
        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    @Override
    public ArrayList<MessageDto> removeDate(TransactionDateDto transactionDateDto) {
        TransactionDatePKEntity transactionDatePKEntity = new TransactionDatePKEntity();
        transactionDatePKEntity.setTransaction(transactionDateDto.getTransaction());
        transactionDatePKEntity.setType(transactionDateDto.getType());
//        TransactionDateEntity transactionDateEntity = transactionDateRepository.getById(transactionDatePKEntity);
        transactionDateRepository.deleteById(transactionDatePKEntity);
        return null;
    }

    @Override
    public ArrayList<TransactionDateDto> getDates(String id) {
        return null;
    }

    @Override
    public ArrayList<TransactionPartnerDto> getPartners(String transactionId) {
        ArrayList<TransactionPartnerDto> partners = new ArrayList<>();
        List<TransactionPartnerEntity> partnerList = transactionPartnerRepository.findPartnerByTransaction(transactionId);
        for (TransactionPartnerEntity transactionPartner : partnerList) {
            TransactionPartnerDto orderPartner = new TransactionPartnerDto();
            orderPartner.setFunction(transactionPartner.getTransactionPartnerPK().getPartnerFunction());
            orderPartner.setPartner(transactionPartner.getTransactionPartnerPK().getPartnerNo());
            orderPartner.setStatus(transactionPartner.getStatus());
            orderPartner.setStatusReason(transactionPartner.getStatusReason());
            orderPartner.setDateAdded(Conversion.dateToString(transactionPartner.getDateAdded()));
            orderPartner.setDateEffective(Conversion.dateToString(transactionPartner.getDateEffective()));
            if (transactionPartner.getCreatedBy() != null) {
                orderPartner.setCreatedBy(transactionPartner.getCreatedBy());
            }
            if (transactionPartner.getChangedBy() != null) {
                orderPartner.setChangedBy(transactionPartner.getChangedBy());
            }
            partners.add(orderPartner);
        }
        return partners;
    }

//    @Override
//    public ArrayList<TransactionDto> getTransactionByApprover(String approver) {
//        TransactionQueryDto transactionQuery = new TransactionQueryDto();
//        ArrayList<TransactionDto> orderObjList = new ArrayList();
//        if (approver != null) {
//            transactionQuery.setPartnerNo(approver);
//            transactionQuery.setPartnerFunction(PartnerFunction.ASSIGNED_APPROVER);
//            ArrayList<TransactionDto> partnerFunctions = search(transactionQuery);
//
//            if (!partnerFunctions.isEmpty()) {
//                for (TransactionDto transactionObj : partnerFunctions) {
//                    TransactionDto transactionDto = getTransaction(transactionObj.getId());
//                    TransactionDto orderObj = new TransactionDto();
//
//                    if (transactionDto != null) {
//
//                        orderObj.setId(transactionDto.getId());
//
//                        if (transactionDto.getDescription() != null) {
//                            orderObj.setDescription(transactionDto.getDescription());
//                        }
//
//                        if (transactionDto.getStatus() != null) {
//                            orderObj.setStatus(transactionDto.getStatus());
//                        }
//
//                        if (transactionDto.getStatusReason() != null) {
//                            orderObj.setStatusReason(transactionDto.getStatusReason());
//                        }
//
//                        if (transactionDto.getSubType() != null) {
//                            orderObj.setSubType(transactionDto.getSubType());
//                        }
//
//                        if (transactionDto.getType() != null) {
//                            orderObj.setType(transactionDto.getType());
//                        }
//
//                        if (transactionDto.getValidFrom() != null) {
//                            orderObj.setValidFrom(transactionDto.getValidFrom());
//                        }
//
//                        if (transactionDto.getValidTo() != null) {
//                            orderObj.setValidTo(transactionDto.getValidTo());
//                        }
//
//                        if (transactionDto.getCreatedBy() != null) {
//                            orderObj.setCreatedBy(transactionDto.getCreatedBy());
//                        }
//
//                        if (transactionDto.getChangedBy() != null) {
//                            orderObj.setChangedBy(transactionDto.getChangedBy());
//                        }
//
//                        orderObjList.add(orderObj);
//                    }
//                }
//            }
//
//        }
//
//        return orderObjList;
//    }

    @Override
    public ArrayList<TransactionDto> search(TransactionQueryDto query) {
        ArrayList<TransactionDto> transactionList = new ArrayList<>();
        if (query.getPartnerFunction() != null && query.getPartnerNo() != null) {
            List<TransactionPartnerEntity> transactions = transactionPartnerRepository.findTransactionByPartner(query.getPartnerNo());
            for (TransactionPartnerEntity txn : transactions) {
                if (txn.getTransactionPartnerPK().getPartnerFunction().equals(query.getPartnerFunction())) {
                    TransactionDto object = getTransaction(txn.getTransactionPartnerPK().getTransactionId());
                    transactionList.add(object);
                }
            }
        }

        if (query.getType() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByType(query.getType());
            for (TransactionEntity txn : transactions) {
                TransactionDto object = getTransaction(txn.getId());
                transactionList.add(object);
            }
        }

        if (query.getStatus() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByStatus(query.getStatus());
            for (TransactionEntity txn : transactions) {
                TransactionDto object = getTransaction(txn.getId());
                transactionList.add(object);
            }
        }

        return transactionList;
    }

    @Override
    public String edit(TransactionDto transactionDto) {
        return null;
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
    public ArrayList<MessageDto> addItem(TransactionItemDto transactionItemDto) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> removeItem(TransactionItemDto transactionItemDto) {
        return null;
    }

    @Override
    public ArrayList<TransactionItemDto> getItems(String id) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> addAmount(TransactionAmountDto transactionAmountDto) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> removeAmount(TransactionAmountDto transactionAmountDto) {
        return null;
    }

    @Override
    public ArrayList<TransactionAmountDto> getAmounts(String id) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> addPartner(TransactionPartnerDto transactionPartnerDto) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> removePartner(TransactionPartnerDto transactionPartnerDto) {
        return null;
    }

//    private TransactionDto getTransaction(String transacationId){
//        TransactionDto object = null;
//        TransactionEntity transaction = transactionRepository.getById(transacationId);
//        if (transaction != null) {
//            object = new TransactionDto();
//            object.setId(transacationId);
//            object.setSubtype(transaction.getSubType());
//            object.setType(transaction.getType());
//            object.setStatus(transaction.getStatus());
//            object.setStatusReason(transaction.getStatusReason());
//            object.setSubtype(transaction.getSubStatus());
//        }
//        return object;
//    }
}
