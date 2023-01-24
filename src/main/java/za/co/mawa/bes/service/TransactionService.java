package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.TransactionPartnerEntity;
import za.co.mawa.bes.object.transaction.TransactionDTO;
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

    @Override
    public String create(TransactionEntity transactionEntity) {

        String txnNo = null;
        try {
            txnNo = numberRange.generateNumber(transactionEntity.getType());
            transactionEntity.setId(txnNo);
            if (transactionEntity.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionRepository.save(transactionEntity);
            OrderDateDto creationDate = new OrderDateDto();
            creationDate.setTransaction(txnNo);
            creationDate.setType(DateType.CREATED);
            addDate(creationDate);
        } catch (Exception ex) {
            return null;
        }

        return txnNo;
    }

    @Override
    public String update(TransactionEntity transactionEntity) {
        return null;
    }

    @Override
    public String delete(String id) {
        return null;
    }

    @Override
    public TransactionEntity findById(String id) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> addDate(OrderDateDto od) {

        TransactionDatePKEntity txnDatePK = new TransactionDatePKEntity();
        txnDatePK.setTransaction(od.getTransaction());
        txnDatePK.setType(od.getType());
        TransactionDateEntity txnDate = new TransactionDateEntity();
        txnDate.setTransactionDatePK(txnDatePK);
        if (od.getValue() != null) {
            txnDate.setValue(Conversion.stringToDate(od.getValue()));
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
    public ArrayList<OrderPartnerDto> getPartners(String transactionId) {
        ArrayList<OrderPartnerDto> partners = new ArrayList<>();
        List<TransactionPartnerEntity> partnerList = transactionPartnerRepository.findPartnerByTransaction(transactionId);
        for (TransactionPartnerEntity transactionPartner : partnerList) {
            OrderPartnerDto orderPartner = new OrderPartnerDto();
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

    @Override
    public ArrayList<OrderHeaderDto> getTransactionByApprover(String approver) {
        TransactionQueryDto transactionQuery = new TransactionQueryDto();
        ArrayList<OrderHeaderDto> orderObjList = new ArrayList();
        if (approver != null) {
            transactionQuery.setPartnerNo(approver);
            transactionQuery.setPartnerFunction(PartnerFunction.ASSIGNED_APPROVER);
            ArrayList<TransactionDTO> partnerFunctions = search(transactionQuery);

            if (!partnerFunctions.isEmpty()) {
                for (TransactionDTO transactionObj : partnerFunctions) {
                    OrderHeaderDto transactionHeader = getHeader(transactionObj.getId());
                    OrderHeaderDto orderObj = new OrderHeaderDto();

                    if (transactionHeader != null) {

                        orderObj.setId(transactionHeader.getId());

                        if (transactionHeader.getDescription() != null) {
                            orderObj.setDescription(transactionHeader.getDescription());
                        }

                        if (transactionHeader.getStatus() != null) {
                            orderObj.setStatus(transactionHeader.getStatus());
                        }

                        if (transactionHeader.getStatusReason() != null) {
                            orderObj.setStatusReason(transactionHeader.getStatusReason());
                        }

                        if (transactionHeader.getSubType() != null) {
                            orderObj.setSubType(transactionHeader.getSubType());
                        }

                        if (transactionHeader.getType() != null) {
                            orderObj.setType(transactionHeader.getType());
                        }

                        if (transactionHeader.getValidFrom() != null) {
                            orderObj.setValidFrom(transactionHeader.getValidFrom());
                        }

                        if (transactionHeader.getValidTo() != null) {
                            orderObj.setValidTo(transactionHeader.getValidTo());
                        }

                        if (transactionHeader.getCreatedBy() != null) {
                            orderObj.setCreatedBy(transactionHeader.getCreatedBy());
                        }

                        if (transactionHeader.getChangedBy() != null) {
                            orderObj.setChangedBy(transactionHeader.getChangedBy());
                        }

                        orderObjList.add(orderObj);
                    }
                }
            }

        }

        return orderObjList;
    }

    @Override
    public ArrayList<TransactionDTO> search(TransactionQueryDto query) {
        ArrayList<TransactionDTO> transactionList = new ArrayList<>();
        if (query.getPartnerFunction() != null && query.getPartnerNo() != null) {
            List<TransactionPartnerEntity> transactions = transactionPartnerRepository.findTransactionByPartner(query.getPartnerNo());
            for (TransactionPartnerEntity txn : transactions) {
                if (txn.getTransactionPartnerPK().getPartnerFunction().equals(query.getPartnerFunction())) {
                    TransactionDTO object = getTransaction(txn.getTransactionPartnerPK().getTransactionId());
                    transactionList.add(object);
                }
            }
        }

        if (query.getType() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByType(query.getType());
            for (TransactionEntity txn : transactions) {
                TransactionDTO object = getTransaction(txn.getId());
                transactionList.add(object);
            }
        }

        if (query.getStatus() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByStatus(query.getStatus());
            for (TransactionEntity txn : transactions) {
                TransactionDTO object = getTransaction(txn.getId());
                transactionList.add(object);
            }
        }

        return transactionList;
    }

    @Override
    public OrderHeaderDto getHeader(String orderId) {
        OrderHeaderDto orderHeader = null;
        TransactionEntity transaction = transactionRepository.getById(orderId);
        if (transaction != null) {
            orderHeader = new OrderHeaderDto();
            orderHeader.setId(transaction.getId());
            if (transaction.getSubType() != null) {
                orderHeader.setSubType(transaction.getSubType());
            }
            if (transaction.getLocation() != null) {
                orderHeader.setLocation(transaction.getLocation());
            }
            if (transaction.getType() != null) {
                orderHeader.setType(transaction.getType());
            }
            orderHeader.setStatus(StringConversion.capitalizeFully(transaction.getStatus().replaceAll("_", " ")));
            if (transaction.getStatusReason() != null) {
                orderHeader.setStatusReason(StringConversion.capitalizeFully(transaction.getStatusReason().replaceAll("_", " ")));
            }
            if (transaction.getSubStatus() != null) {
                orderHeader.setSubStatus(StringConversion.capitalizeFully(transaction.getSubStatus()));
            }
            if (transaction.getDescription() != null) {
                orderHeader.setDescription(transaction.getDescription());
            }
            if (transaction.getSubDescription() != null) {
                orderHeader.setSubDescription(transaction.getSubDescription());
            }
            if (transaction.getValidTo() != null) {
                orderHeader.setValidTo(Conversion.dateToString(transaction.getValidTo()));
            }
            if (transaction.getValidFrom() != null) {
                orderHeader.setValidFrom(Conversion.dateToString(transaction.getValidFrom()));
            }
            if (transaction.getCreatedBy() != null) {
                orderHeader.setCreatedBy(transaction.getCreatedBy());
            }
            if (transaction.getChangedBy() != null) {
                orderHeader.setChangedBy(transaction.getChangedBy());
            }
        }
        return orderHeader;
    }

    private TransactionDTO getTransaction(String transacationId){
        TransactionDTO object = null;
        TransactionEntity transaction = transactionRepository.getById(transacationId);
        if (transaction != null) {
            object = new TransactionDTO();
            object.setId(transacationId);
            object.setSubtype(transaction.getSubType());
            object.setType(transaction.getType());
            object.setStatus(transaction.getStatus());
            object.setStatusReason(transaction.getStatusReason());
            object.setSubtype(transaction.getSubStatus());
        }
        return object;
    }
}
