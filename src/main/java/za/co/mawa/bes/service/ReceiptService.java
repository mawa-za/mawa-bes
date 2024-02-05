package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
//import javax.sql.rowset.Predicate;
import jakarta.persistence.criteria.Predicate;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributePKEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.DoesNotExist;

import za.co.mawa.bes.dao.ReceiptDao;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.entity.ReceiptEntity;
import za.co.mawa.bes.repository.TransactionAttributeRepository;
import za.co.mawa.bes.repository.TransactionLinkRepository;
import za.co.mawa.bes.utils.NumberRangeType;
import za.co.mawa.bes.repository.ReceiptRepository;
import za.co.mawa.bes.utils.ReceiptType;
import za.co.mawa.bes.utils.TransactionAttribute;
import za.co.mawa.bes.utils.TransactionType;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;

@Service
public class ReceiptService implements ReceiptDao {

    @Autowired
    ReceiptRepository receiptRepository;
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    TransactionLinkRepository transactionLinkRepository;

    @Autowired
    TransactionAttributeRepository transactionAttributeRepository;
    @Autowired
    UserService userService;

    @Override
    public ReceiptDto createReceipt(ReceiptCreateDto receipt) throws Exception {
        try {
            ReceiptEntity entity = new ReceiptEntity();
            entity.setReceiptType(receipt.getReceiptType().toUpperCase());
            entity.setReceiptNumber(numberRangeService.generateNumber(NumberRangeType.RECEIPT));
            entity.setLocation(receipt.getLocation());
            entity.setCreationDate(new Date());
            entity.setCreationTime(new Date());
            entity.setCreatedBy(userService.getUserByName(getUser()).getPartner());
            entity.setTransaction(receipt.getTransaction());
            entity.setTenderType(receipt.getTenderType().toUpperCase());
            entity.setAmount(new BigDecimal(receipt.getAmount()));
            return entityIDtoDto(receiptRepository.save(entity));
        } catch (Exception e) {
            throw new Exception();
        }
    }

    @Override
    public ReceiptDto getReceipt(String id) throws DoesNotExist {
        ReceiptEntity entity = receiptRepository.getById(id);
        if (entity != null) {
            SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
            ReceiptDto receipt = new ReceiptDto();
            receipt.setId(entity.getId());
            receipt.setReceiptNumber(entity.getReceiptNumber());
            receipt.setTransaction(entity.getTransaction());
            receipt.setReceiptType(entity.getReceiptType());
            receipt.setTenderType(entity.getTenderType());
            receipt.setAmount(entity.getAmount().toString());
            receipt.setCreatedBy(entity.getCreatedBy());
            receipt.setCreationDate(formatterDate.format(entity.getCreationDate()));
            receipt.setCreationDate(formatterTime.format(entity.getCreationTime()));

            return receipt;
        } else {
            throw new DoesNotExist();
        }
    }

    @Override
    public ArrayList<ReceiptDto> getReceipts(ReceiptSearchDto receiptSearch) throws Exception {
        ArrayList<ReceiptDto> receiptDtos = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<ReceiptEntity> receipts = receiptRepository.findAll(findByCriteria(receiptSearch), sort);
        receiptDtos = entityArrayToDto(receipts);
        return receiptDtos;
    }

    @Override
    public ArrayList<ReceiptDto> getReceiptsX(ReceiptSearchDto receiptSearch) throws Exception {
        ArrayList<ReceiptDto> receiptDtos = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<ReceiptEntity> receipts = receiptRepository.findAll(findByCriteria(receiptSearch), sort);
        List<ReceiptEntity> receiptsNotCashed = new ArrayList<>();
        for (ReceiptEntity receipt : receipts) {
            TransactionLinkEntity linkEntity = transactionLinkRepository.getTransactionLinks(receipt.getId(), TransactionType.CASHUP);
            if (linkEntity == null) {
                receiptsNotCashed.add(receipt);
            }
        }
        receiptDtos = entityArrayToDto(receiptsNotCashed);
        return receiptDtos;
    }

    private ReceiptDto entityIDtoDto(ReceiptEntity entity) throws Exception {
        try {
            ReceiptDto receipt = new ReceiptDto();
            receipt.setId(entity.getId());
            return receipt;
        } catch (Exception e) {
            throw new Exception();
        }

    }

    private String getUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }

    private Specification<ReceiptEntity> findByCriteria(ReceiptSearchDto receiptSearchDto) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (receiptSearchDto.getTransaction() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("transaction"), receiptSearchDto.getTransaction()));
            }
            if (receiptSearchDto.getReceiptType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("receiptType"), receiptSearchDto.getReceiptType()));
            }
            if (receiptSearchDto.getInvoiceNumber() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("invoiceNumber"), receiptSearchDto.getInvoiceNumber()));
            }
            if (receiptSearchDto.getCreatedBy() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("createdBy"), receiptSearchDto.getCreatedBy()));
            }
            if (receiptSearchDto.getMembershipNumber() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("membershipNumber"), receiptSearchDto.getMembershipNumber()));
            }
            if (receiptSearchDto.getMembershipPeriod() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("membershipPeriod"), receiptSearchDto.getMembershipPeriod()));
            }
            if (receiptSearchDto.getTenderType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("tenderType"), receiptSearchDto.getTenderType()));
            }
            return predicate;
        };
    }

    private ArrayList<ReceiptDto> entityArrayToDto(List<ReceiptEntity> receipts) throws Exception {
        ArrayList<ReceiptDto> receiptDt = new ArrayList<>();
        for (ReceiptEntity receipt : receipts) {
            try {
                receiptDt.add(entityToDto(receipt));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return receiptDt;
    }

    private ReceiptDto entityToDto(ReceiptEntity entity) throws Exception {
        try {
            SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
            ReceiptDto receipt = new ReceiptDto();
            receipt.setId(entity.getId());
            receipt.setReceiptNumber(entity.getReceiptNumber());
            receipt.setTransaction(entity.getTransaction());
            receipt.setReceiptType(entity.getReceiptType());
            receipt.setTenderType(entity.getTenderType());
            receipt.setAmount(entity.getAmount().toString());
            receipt.setCreatedBy(entity.getCreatedBy());
            receipt.setCreationDate(formatterDate.format(entity.getCreationDate()));
            receipt.setCreationTime(formatterTime.format(entity.getCreationTime()));

            return receipt;
        } catch (Exception e) {
            throw new Exception();
        }
    }

    private String determinePeriod(String id) {
        try {
            List<TransactionAttributeEntity> transactionAttributeEntityList = transactionAttributeRepository.find(id,TransactionAttribute.LAST_PREMIUM_PERIOD);
            TransactionAttributeEntity transactionAttributeEntity = transactionAttributeEntityList.iterator().next();
            String previousPeriod =  transactionAttributeEntity.getValue();
            String yearString = previousPeriod.substring(0,4);
            String monthString = previousPeriod.substring(4,6);
            if (Integer.parseInt(monthString) == 12){
                yearString = (Integer.toString(Integer.parseInt(yearString) + 1));
                monthString = "01";
            }else{
                int month = Integer.parseInt(monthString) + 1;
                monthString = String.format("%02d", month);
            }
            transactionAttributeEntity.setValue(yearString + monthString);
            transactionAttributeEntity.setValidTo(new Date());
            transactionAttributeEntity.setValidFrom(new Date());
            transactionAttributeRepository.save(transactionAttributeEntity);
            return transactionAttributeEntity.getValue();
        } catch (Exception exception) {
            TransactionAttributeEntity transactionAttributeEntity = new TransactionAttributeEntity();
            transactionAttributeEntity.setTransaction(id);
            transactionAttributeEntity.setAttribute(TransactionAttribute.LAST_PREMIUM_PERIOD);
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            String monthString = String.format("%02d", month);
            transactionAttributeEntity.setValue(Integer.toString(year) + monthString);
            transactionAttributeEntity.setValidTo(new Date());
            transactionAttributeEntity.setValidFrom(new Date());
            transactionAttributeRepository.save(transactionAttributeEntity);
            return transactionAttributeEntity.getValue();
        }
    }
}
