package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
//import javax.sql.rowset.Predicate;
import jakarta.persistence.criteria.Predicate;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributePKEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.DoesNotExist;

import za.co.mawa.bes.dao.ReceiptDao;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.entity.ReceiptEntity;
import za.co.mawa.bes.exception.DuplicateCreationException;
import za.co.mawa.bes.repository.TransactionAttributeRepository;
import za.co.mawa.bes.repository.TransactionLinkRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.repository.ReceiptRepository;

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
    FieldOptionService fieldOptionService;
    @Autowired
    TransactionAttributeRepository transactionAttributeRepository;
    @Autowired
    UserService userService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PartnerService partnerService;

    @Override
    public ReceiptDto createReceipt(ReceiptCreateDto receipt) throws Exception {
        try {
            ReceiptEntity entity = new ReceiptEntity();
            entity.setReceiptType(receipt.getReceiptType().toUpperCase());
            entity.setReceiptNumber(numberRangeService.generateNumber(NumberRangeType.RECEIPT));
            entity.setExtReceiptNumber(null);

            if(!StringUtils.isBlank(receipt.getExternalReceiptNo())) {
                if(receiptRepository.existsByExtReceiptNumber(receipt.getExternalReceiptNo())){
                    throw new DuplicateCreationException("Duplicate receipt number");
                }

                entity.setExtReceiptNumber(receipt.getExternalReceiptNo());
            }
            entity.setLocation(receipt.getLocation());
            entity.setCreationDate(new Date());
            entity.setCreationTime(new Date());
            entity.setCreatedBy(getUser());
            entity.setTransaction(receipt.getTransaction());
            entity.setTenderType(receipt.getTenderType().toUpperCase());
            entity.setAmount(receipt.getAmount());

            ReceiptEntity newEntity = receiptRepository.save(entity);
            return getReceipt(newEntity.getId());

        } catch (Exception e) {
            throw new Exception(e);
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
            receipt.setExternalReceiptNo(entity.getExtReceiptNumber());
            receipt.setReceiptType(fieldOptionService.getFieldOption(Field.RECEIPT_TYPE, entity.getReceiptType()));
            receipt.setTenderType(fieldOptionService.getFieldOption(Field.TENDER_TYPE, entity.getTenderType()));
            receipt.setAmount(entity.getAmount());
            try {
                receipt.setTransaction(transactionService.get(entity.getTransaction()));
                receipt.setCreatedBy(partnerService.get(entity.getCreatedBy()));
            } catch (Exception e) {

            }
            receipt.setCreationDate(formatterDate.format(entity.getCreationDate()));
            receipt.setCreationTime(formatterTime.format(entity.getCreationTime()));

            return receipt;
        } else {
            throw new DoesNotExist();
        }
    }

    @Override
    public ArrayList<ReceiptDto> getReceipts(ReceiptSearchDto receiptSearch) {
        ArrayList<ReceiptDto> receiptDtos = new ArrayList<>();
        Sort sort = Sort.by("id").ascending();
        List<ReceiptEntity> receipts = receiptRepository.findAll(findByCriteria(receiptSearch), sort);
        for (ReceiptEntity receiptEntity : receipts) {
            try {
                receiptDtos.add(getReceipt(receiptEntity.getId()));
            } catch (Exception e) {

            }
        }
        return receiptDtos;
    }

    @Override
    public ArrayList<ReceiptDto> getReceiptsX(ReceiptSearchDto receiptSearch) throws Exception {
        ArrayList<ReceiptDto> receiptDtos = new ArrayList<>();
        Sort sort = Sort.by("id").ascending();
        List<ReceiptEntity> receipts = receiptRepository.findAll(findByCriteria(receiptSearch), sort);
        for (ReceiptEntity receipt : receipts) {
            try {
                TransactionLinkEntity linkEntity = transactionLinkRepository.getTransactionLinks(receipt.getId(), TransactionType.CASHUP);
                if (linkEntity == null) {
                    try {
                        receiptDtos.add(getReceipt(receipt.getId()));
                    } catch (Exception e) {

                    }
                }
            }catch (Exception e){}
        }
        return receiptDtos;
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
            if (receiptSearchDto.getLocation() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("location"), receiptSearchDto.getLocation()));
            }
            if (receiptSearchDto.getTenderType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("tenderType"), receiptSearchDto.getTenderType()));
            }
            return predicate;
        };
    }

//    private ArrayList<ReceiptDto> entityArrayToDto(List<ReceiptEntity> receipts) {
//        ArrayList<ReceiptDto> receiptDt = new ArrayList<>();
//        for (ReceiptEntity receipt : receipts) {
//            try {
//                receiptDt.add(entityToDto(receipt));
//            } catch (Exception e) {
////                throw new RuntimeException(e);
//            }
//        }
//        return receiptDt;
//    }

    public String getUser() {
        try {

            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserEntity user = userRepository.getByName(userDetails.getUsername());

            if (user != null) {

                return String.valueOf(user.getPartner());
            } else {
                return null;
            }
        } catch (Exception e) {

            return null;
        }
    }
}
