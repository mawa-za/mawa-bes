package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.CashupDao;
import za.co.mawa.bes.dto.cashup.CashupCreateDto;
import za.co.mawa.bes.dto.cashup.CashupDto;
import za.co.mawa.bes.dto.cashup.CashupEditDto;
import za.co.mawa.bes.dto.cashup.CashupSearchDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.TransactionAmountRepository;
import za.co.mawa.bes.repository.TransactionItemRepository;
import za.co.mawa.bes.repository.TransactionLinkRepository;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class CashupService implements CashupDao {
    @Autowired
    ReceiptService receiptService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionAmountRepository transactionAmountRepository;
    @Autowired
    UserService userService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerService partnerService;

    @Override
    public String create(CashupCreateDto cashupCreateDto) throws Exception {
        ArrayList<ReceiptDto> receiptsFiltered = new ArrayList<>();
        ReceiptSearchDto searchReceipt = new ReceiptSearchDto();
        searchReceipt.setCreatedBy(cashupCreateDto.getEmployeeResponsibleId());
        searchReceipt.setLocation(cashupCreateDto.getSalesArea());
        ArrayList<ReceiptDto> receipts = receiptService.getReceiptsX(searchReceipt);
        String id = null;
        if (cashupCreateDto.getReceipts().size() > 0) {
            receiptsFiltered = receipts.stream().filter(obj -> cashupCreateDto.getReceipts().contains(obj.getId())).collect(Collectors.toCollection(ArrayList::new));
        } else {
            receiptsFiltered = receipts;
        }
        if (receiptsFiltered.size() > 0) {
            BigDecimal amount = BigDecimal.ZERO;
            for (ReceiptDto receipt : receipts) {
                amount = amount.add(receipt.getAmount());
            }
            if (!BigDecimal.ZERO.equals(amount)) {
                TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
                transactionCreateDto.setLocation(cashupCreateDto.getSalesArea());
                transactionCreateDto.setType(TransactionType.CASHUP);
                transactionCreateDto.setStatus(Status.OPEN);
                TransactionDto transaction = transactionService.create(transactionCreateDto);
                if (transaction.getId() != null) {
                    id = transaction.getId();

                    try {
                        TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                        transactionAmountInboundDto.setAmount(amount);
                        transactionAmountInboundDto.setTransaction(transaction.getId());
                        transactionAmountInboundDto.setType(AmountType.AMOUNT_COLLECTED);
                        transactionAmountService.save(transactionAmountInboundDto);
                    } catch (Exception exception) {

                    }

                    try {
                        TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                        transactionAmountInboundDto.setAmount(BigDecimal.ZERO);
                        transactionAmountInboundDto.setTransaction(transaction.getId());
                        transactionAmountInboundDto.setType(AmountType.AMOUNT_DEPOSITED);
                        transactionAmountService.save(transactionAmountInboundDto);
                    } catch (Exception exception) {

                    }
                    for (ReceiptDto receipt : receipts) {
                        TransactionLinkDto link = new TransactionLinkDto();
                        link.setTransaction1(transaction.getId());
                        link.setTransaction2(receipt.getId());
                        link.setType(TransactionType.CASHUP);
                        link.setCreateBy(getUser());
                        transactionService.addLink(link);
                    }
                    TransactionPartnerDto employee = new TransactionPartnerDto();
                    employee.setFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
                    employee.setTransaction(transaction.getId());
                    employee.setPartner(cashupCreateDto.getEmployeeResponsibleId());
                    employee.setStatus(Status.ACTIVE);
                    transactionService.addPartner(employee);

                    TransactionDateDto date = new TransactionDateDto();
                    date.setType(DateType.CREATED);
                    date.setTransaction(id);
                    date.setValue(new Date());
                    transactionService.addDate(date);

                    date = new TransactionDateDto();
                    date.setType(DateType.LAST_UPDATED);
                    date.setTransaction(id);
                    date.setValue(new Date());
                    transactionService.addDate(date);

                }
            }
        }
        return id;
    }

    public String createNoCash(CashupCreateDto cashupCreateDto) throws Exception {
        ReceiptSearchDto searchReceipt = new ReceiptSearchDto();
        searchReceipt.setCreatedBy(cashupCreateDto.getEmployeeResponsibleId());
        searchReceipt.setLocation(cashupCreateDto.getSalesArea());
        String id = null;
        if (!BigDecimal.ZERO.equals(cashupCreateDto.getAmount())) {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setLocation(cashupCreateDto.getSalesArea());
            transactionCreateDto.setType(TransactionType.CASHUP);
            transactionCreateDto.setStatus(Status.CLOSED);
            TransactionDto transaction = transactionService.create(transactionCreateDto);
            if (transaction.getId() != null) {
                try {
                    TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                    transactionAmountInboundDto.setAmount(cashupCreateDto.getAmount());
                    transactionAmountInboundDto.setTransaction(transaction.getId());
                    transactionAmountInboundDto.setType(AmountType.AMOUNT_COLLECTED);
                    transactionAmountService.save(transactionAmountInboundDto);
                } catch (Exception exception) {

                }

                for (String receipt : cashupCreateDto.getReceipts()) {
                    TransactionLinkDto link = new TransactionLinkDto();
                    link.setTransaction1(transaction.getId());
                    link.setTransaction2(receipt);
                    link.setType(TransactionType.CASHUP);
                    link.setCreateBy(getUser());
                    transactionService.addLink(link);
                }
                TransactionDateDto date = new TransactionDateDto();
                date.setType(DateType.CREATED);
                date.setTransaction(id);
                date.setValue(new Date());
                transactionService.addDate(date);

                date = new TransactionDateDto();
                date.setType(DateType.LAST_UPDATED);
                date.setTransaction(id);
                date.setValue(new Date());
                transactionService.addDate(date);

            }
        }
        return id;
    }

    @Override
    public CashupDto get(String id) throws Exception, DoesNotExist {
        TransactionDto transactionDto = transactionService.get(id);
        if (transactionDto.getType().equalsIgnoreCase(TransactionType.CASHUP)) {
            try {
                CashupDto cashupDto = new CashupDto();
                ArrayList<ReceiptDto> receipts = new ArrayList<>();
                try {
                    cashupDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
                } catch (Exception e) {

                }
                try {
                    cashupDto.setChangedBy(userService.getUserByName(transactionDto.getChangedBy()).getPartner());
                } catch (Exception e) {

                }
                cashupDto.setId(transactionDto.getId());
                cashupDto.setNumber(transactionDto.getNumber());
                cashupDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
                cashupDto.setSalesArea(fieldOptionService.getFieldOption(Field.BRANCH, transactionDto.getLocation()));
                List<TransactionLinkDto> links = transactionService.getLinks(id);
                for (TransactionLinkDto link : links) {
                    ReceiptDto receipt = new ReceiptDto();
                    receipt = receiptService.getReceipt(link.getTransaction2());
                    receipts.add(receipt);
                }
                cashupDto.setReceipts(receipts);

                for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
                    if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                        cashupDto.setEmployeeResponsible(partnerService.get(transactionPartner.getPartner()));
                    }
                }
                try {
                    cashupDto.setAmountCollected(transactionAmountService.getByTransaction(id).stream()
                            .filter(a -> Objects.equals(a.getType().getCode(), AmountType.AMOUNT_COLLECTED))
                            .toList().iterator().next().getAmount());
                } catch (Exception exception) {
                }
                try {
                    cashupDto.setAmountDeposited(transactionAmountService.getByTransaction(id).stream()
                            .filter(a -> Objects.equals(a.getType().getCode(), AmountType.AMOUNT_DEPOSITED))
                            .toList().iterator().next().getAmount());
                } catch (Exception exception) {
                }

                for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                    if (transactionDateDto.getType().equalsIgnoreCase(DateType.CREATED)) {
                        cashupDto.setCreatedDate(transactionDateDto.getValue());
                    }
                    if (transactionDateDto.getType().equalsIgnoreCase(DateType.LAST_UPDATED)) {
                        cashupDto.setLastUpdated(transactionDateDto.getValue());
                    }
                }
                return cashupDto;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        } else {
            throw new DoesNotExist();
        }

    }

    @Override
    public CashupDto getCashups(CashupSearchDto search) throws Exception {

        return null;
    }

    @Override
    public boolean edit(CashupEditDto edit, String id) throws Exception {
        TransactionEntity entity = transactionRepository.getById(id);
        boolean edited = false;
        if (entity != null) {
            try {
                if (edit.getStatus() != null) {
                    entity.setStatus(edit.getStatus());
                }
                if (edit.getAmountDeposited() != null) {
                    try {
                        TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                        transactionAmountInboundDto.setAmount(new BigDecimal(edit.getAmountDeposited()));
                        transactionAmountInboundDto.setTransaction(id);
                        transactionAmountInboundDto.setType(AmountType.AMOUNT_DEPOSITED);
                        transactionAmountService.save(transactionAmountInboundDto);
                    } catch (Exception exception) {

                    }
                }
                TransactionDateEdit editDate = new TransactionDateEdit();
                editDate.setTransaction(id);
                editDate.setType(DateType.LAST_UPDATED);
                editDate.setValue(new Date());
                transactionService.dateEdit(editDate);
                entity.setChangedBy(getUser());
                transactionRepository.save(entity);
                edited = true;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        } else {
            throw new TransactionNotFound();
        }
        return edited;
    }

    private String getUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }
}
