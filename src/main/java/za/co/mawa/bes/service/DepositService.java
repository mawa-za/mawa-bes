package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.DepositDao;
import za.co.mawa.bes.dto.cashup.CashupDto;
import za.co.mawa.bes.dto.cashup.CashupEditDto;
import za.co.mawa.bes.dto.deposit.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.transaction.TransactionDatePKEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkPKEntity;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DepositService implements DepositDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    CashupService cashupService;
    @Autowired
    TransactionLinkRepository transactionLinkRepository;
    @Autowired
    TransactionAmountRepository transactionAmountRepository;
    @Autowired
    TransactionDateRepository transactionDateRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AttachmentRepository attachmentRepository;
    @Autowired
    AttachmentService attachmentService;
    @Autowired
    UserService userService;


    public String create(DepositCreateDto depositCreateDto) throws Exception {
        try{
            TransactionCreateDto createDto = new TransactionCreateDto();
            createDto.setType(TransactionType.DEPOSIT);
            createDto.setStatus(Status.NEW);
            TransactionDto transaction = transactionService.create(createDto);
            if(transaction.getId() != null)
            {
                TransactionDateDto date = new TransactionDateDto();
                date.setType(DateType.CREATED);
                date.setTransaction(transaction.getId());
                date.setValue(new Date());
                transactionService.addDate(date);
                BigDecimal amount = new BigDecimal(depositCreateDto.getAmount());
                try {
                    TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                    transactionAmountInboundDto.setAmount(amount);
                    transactionAmountInboundDto.setTransaction(transaction.getId());
                    transactionAmountInboundDto.setType(AmountType.DEPOSIT_AMOUNT);
                    transactionAmountService.save(transactionAmountInboundDto);
                } catch (Exception exception) {

                }
                TransactionDto transactionLink = transactionService.get(depositCreateDto.getTransactionIdLink());

                TransactionLinkDto linkDto = new TransactionLinkDto();
                linkDto.setTransaction1(transaction.getId());
                linkDto.setTransaction2(transactionLink.getId());
                linkDto.setType(TransactionType.DEPOSIT);
                linkDto.setCreateBy(getUser());
                transactionService.addLink(linkDto);

                if(transactionLink.getType().equalsIgnoreCase(TransactionType.CASHUP)){
                    CashupEditDto edit = new CashupEditDto();
                    CashupDto cashupDto = cashupService.get(transactionLink.getId());
                    BigDecimal total = cashupDto.getAmountDeposited().add(amount);
                    edit.setAmountDeposited(total.toString());
                    if(total.compareTo(cashupDto.getAmountCollected()) == 0){
                        edit.setStatus(Status.CLOSED);
                    }
                    cashupService.edit(edit,transactionLink.getId());
                }
            }
            return transaction.getId();
        }catch (Exception ex){
          throw new RuntimeException(ex);
        }
    }

    public String createDepositAttachment(DepositAttachmentCreateDto depositAttachmentCreateDto) throws Exception {
        try{
            TransactionCreateDto createDto = new TransactionCreateDto();
            createDto.setType(TransactionType.DEPOSIT);
            createDto.setStatus(Status.NEW);
//            createDto.setStatus(Status.NEW);
            TransactionDto transaction = transactionService.create(createDto);
            if(transaction.getId() != null)
            {
                TransactionDateDto date = new TransactionDateDto();
                date.setType(DateType.CREATED);
                date.setTransaction(transaction.getId());
                date.setValue(new Date());
                transactionService.addDate(date);
                BigDecimal amount = new BigDecimal(depositAttachmentCreateDto.getAmount());
                try {
                    TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                    transactionAmountInboundDto.setAmount(amount);
                    transactionAmountInboundDto.setTransaction(transaction.getId());
                    transactionAmountInboundDto.setType(AmountType.DEPOSIT_AMOUNT);
                    transactionAmountService.save(transactionAmountInboundDto);
                } catch (Exception exception) {

                }
                TransactionDto transactionLink = transactionService.get(depositAttachmentCreateDto.getTransactionIdLink());

                TransactionLinkDto linkDto = new TransactionLinkDto();
                linkDto.setTransaction1(transaction.getId());
                linkDto.setTransaction2(transactionLink.getId());
                linkDto.setType(TransactionType.DEPOSIT);
                linkDto.setCreateBy(getUser());
                transactionService.addLink(linkDto);

                try {
                    AttachmentEntity attachmentEntity = new AttachmentEntity();
                    attachmentEntity.setFile(Base64.getDecoder().decode(depositAttachmentCreateDto.getFile()));
                    attachmentEntity.setUploadBy(UserContext.getCurrentUser());
                    attachmentEntity.setUploadDate(new Date());
                    attachmentEntity.setUploadTime(new Date());
                    attachmentEntity.setDocumentType(depositAttachmentCreateDto.getDocumentType());
                    attachmentEntity.setObjectId(depositAttachmentCreateDto.getObjectId());
                    attachmentEntity.setExtension(depositAttachmentCreateDto.getExtension());
                    AttachmentEntity attachment = attachmentRepository.save(attachmentEntity);

                    TransactionLinkDto linkAttachmentDto = new TransactionLinkDto();
                    linkAttachmentDto.setTransaction1(transaction.getId());
                    linkAttachmentDto.setTransaction2(attachment.getId());
                    linkAttachmentDto.setType(TransactionType.DEPOSIT_ATTACHMENT);
                    linkAttachmentDto.setCreateBy(getUser());
                    transactionService.addLink(linkAttachmentDto);
                } catch (Exception e) {
//                    throw new RuntimeException(e);
                }
                if(transactionLink.getType().equalsIgnoreCase(TransactionType.CASHUP)){
                    CashupEditDto edit = new CashupEditDto();
                    CashupDto cashupDto = cashupService.get(transactionLink.getId());
                    BigDecimal total = cashupDto.getAmountDeposited().add(amount);
                    edit.setAmountDeposited(total.toString());
                    if(total.compareTo(cashupDto.getAmountCollected()) == 0){
                        edit.setStatus(Status.CLOSED);
                    }
                    cashupService.edit(edit,transactionLink.getId());
                }
            }
            return transaction.getId();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public DepositDto get(String id) throws Exception {
        try{
            DepositDto depositDto = new DepositDto();
            TransactionDto transactionDto = transactionService.get(id);

            depositDto.setId(transactionDto.getId());
            depositDto.setNumber(transactionDto.getNumber());
            try {
                depositDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
            }catch (Exception e){}

            depositDto.setStatus(transactionDto.getStatus());
            depositDto.setChangedBy(transactionDto.getChangedBy());
            for(TransactionDateDto date:transactionService.getDates(id)){
                if(date.getType().equalsIgnoreCase(DateType.CREATED)){
                    depositDto.setCreatedOn(Conversion.dateToString(date.getValue()));
                    break;
                }
            }
            for(TransactionLinkDto link:transactionService.getLinks(id)){
                if(link.getType().equalsIgnoreCase(TransactionType.DEPOSIT)){
                    depositDto.setTransactionIdLink(link.getTransaction2());

                }else if(link.getType().equalsIgnoreCase(TransactionType.DEPOSIT_ATTACHMENT)){
                    try {
                        depositDto.setAttachment(attachmentService.getOne(link.getTransaction2()));
                    } catch (Exception e) {
//                        throw new RuntimeException(e);
                    }
                }
            }
            try {
                depositDto.setAmount(transactionAmountService.getByTransaction(id).stream()
                        .toList().iterator().next().getAmount().toString());
                        //amounts are not being returned due to this filter
                        //.filter(a -> Objects.equals(a.getType().getCode(), AmountType.DEPOSIT_AMOUNT))
                        //.filter(a -> Objects.equals(a.getType().getCode().toUpperCase(), AmountType.DEPOSIT_AMOUNT))
                        //.toList().iterator().next().getAmount().toString());
            } catch (Exception exception) {
            }
            //depositDto.setAmount();
            return depositDto;
        }catch (Exception ex){
           throw new RuntimeException(ex);
        }

    }

    @Override
    public ArrayList<DepositDto> search(DepositSearchDto searchDto) throws Exception {
        try{
            ArrayList<DepositDto> deposits = new ArrayList<>();
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.DEPOSIT);
            if(searchDto.getCreatedOn() != null){
                Date create = new SimpleDateFormat("yyyy-MM-dd").parse(searchDto.getCreatedOn());
                transactionQueryDto.setValue(create);
                transactionQueryDto.setDateType(DateType.CREATED);
            }
            if(searchDto.getCreatedBy() != null){
                transactionQueryDto.setCreatedBy(searchDto.getCreatedBy());
            }
            if(searchDto.getStatus() != null) {
                transactionQueryDto.setStatus(searchDto.getStatus());
            }
            for(String id:transactionService.search(transactionQueryDto)) {
                DepositDto depositDto = new DepositDto();
                depositDto = get(id);
                deposits.add(depositDto);
            }
            return deposits;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
    @Override
    public boolean delete(String id) throws Exception {
        try{
            for(TransactionDateDto dateDto: transactionService.getDates(id)){
                TransactionDatePKEntity transactionDatePKEntity = new TransactionDatePKEntity();
                transactionDatePKEntity.setTransaction(dateDto.getTransaction());
                transactionDatePKEntity.setType(dateDto.getType());
                transactionDateRepository.deleteById(transactionDatePKEntity);
            }
             BigDecimal amount =  BigDecimal.ZERO;

            String transaction = "";
            for(TransactionLinkDto link:transactionService.getLinks(id)){
                if(link.getType().equalsIgnoreCase(TransactionType.DEPOSIT)){
                    transaction = link.getTransaction2();
                }
                if(link.getType().equalsIgnoreCase(TransactionType.DEPOSIT_ATTACHMENT)){
                    try {
                        attachmentRepository.deleteById(link.getTransaction2());
                    } catch (Exception e) {
                    }
                }
                TransactionLinkPKEntity pk = new TransactionLinkPKEntity();
                pk.setType(link.getType());
                pk.setTransaction1(link.getTransaction1());
                pk.setTransaction2(link.getTransaction2());
                transactionLinkRepository.deleteById(pk);
            }
            transactionService.delete(id);
            TransactionDto transactionDto = transactionService.get(transaction);
            if(transactionDto.getType().equalsIgnoreCase(TransactionType.CASHUP)){
                CashupDto cashupDto = cashupService.get(transaction);
                BigDecimal total = cashupDto.getAmountDeposited().subtract(amount);

                CashupEditDto edit = new CashupEditDto();
                edit.setAmountDeposited(total.toString());
                if(cashupDto.getAmountCollected().compareTo(total) > 0){
                    edit.setStatus(Status.OPEN);
                }
                cashupService.edit(edit,transaction);
            }
            return true;
        }catch (Exception exception){
            throw new RuntimeException(exception);
        }
    }

    @Override
    public ArrayList<DepositDto> searchByTransactionLink(String linkId) {
        try{
            ArrayList<DepositDto> deposits = new ArrayList<>();
            for(TransactionLinkEntity link : transactionLinkRepository.getTransactionLink(linkId,TransactionType.DEPOSIT)){
              DepositDto deposit = new DepositDto();
              deposit = get(link.getTransactionLinkPKEntity().getTransaction1());
              deposits.add(deposit);
            }
            return deposits;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private String getUser()
    {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }

    public String getUserPartnerId() {
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
