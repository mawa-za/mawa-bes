package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.VoucherDao;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.voucher.*;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class VoucherService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    UserService userService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerRepository partnerRepository;
    @Autowired
    TransactionRepository transactionRepository;

    public VoucherOutboundDto create(VoucherInboundDto voucherInboundDto) throws Exception {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.VOUCHER);
            transactionCreateDto.setSubType(voucherInboundDto.getType());
            transactionCreateDto.setStatus(Status.ACTIVE);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            try {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(voucherInboundDto.getAmount());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.VOUCHER_AMOUNT);
                transactionAmountService.save(transactionAmountInboundDto);
            } catch (Exception exception) {

            }
            if (voucherInboundDto.getContractId() != null && voucherInboundDto.getContractId() != "") {
                TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                transactionLinkDto.setTransaction1(voucherInboundDto.getContractId());
                transactionLinkDto.setTransaction2(transactionDto.getId());
                transactionLinkDto.setType(TransactionType.VOUCHER);
                transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                transactionService.addLink(transactionLinkDto);
            }
            if (voucherInboundDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(voucherInboundDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (voucherInboundDto.getRecipientId() != null){
                TransactionPartnerDto partner = new TransactionPartnerDto();
                partner.setTransaction(transactionDto.getId());
                partner.setFunction(PartnerFunction.RECIPIENT);
                partner.setPartner(voucherInboundDto.getRecipientId());
                transactionService.addPartner(partner);
            }

            TransactionDateDto dateCreate = new TransactionDateDto();
            dateCreate.setValue(new Date());
            dateCreate.setTransaction(transactionDto.getId());
            dateCreate.setType(DateType.CREATED);
            transactionService.addDate(dateCreate);

            TransactionDateDto dateDto = new TransactionDateDto();
            dateDto.setValue(Conversion.stringToDate("9999-12-31"));
            dateDto.setTransaction(transactionDto.getId());
            dateDto.setType(DateType.EXPIRY_DATE);
            transactionService.addDate(dateDto);
            return get(transactionDto.getId());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

    }

    public VoucherOutboundDto get(String id) throws Exception {
        try {
            VoucherOutboundDto voucherOutboundDto = new VoucherOutboundDto();
            TransactionDto transactionDto = transactionService.get(id);
            if (transactionDto != null) {

                voucherOutboundDto.setId(transactionDto.getId());
                voucherOutboundDto.setNumber(transactionDto.getNumber());
                voucherOutboundDto.setStatus(transactionDto.getStatus());
//                voucherOutboundDto.setStatusReason(transactionDto.getStatusReason());

                voucherOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
                voucherOutboundDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
                voucherOutboundDto.setChangedBy(transactionDto.getChangedBy());
//                voucherOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON,transactionDto.getStatusReason()));

                voucherOutboundDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
                // Retrieve dates
                for (TransactionDateDto dates : transactionService.getDates(id)) {
                    if (dates.getType().equalsIgnoreCase(DateType.CREATED)) {
                        voucherOutboundDto.setDateCreated(Conversion.dateToString(dates.getValue()));
                    }
                    if (dates.getType().equalsIgnoreCase(DateType.EXPIRY_DATE)) {
                        voucherOutboundDto.setExpiryDate(Conversion.dateToString(dates.getValue()));
                    }
                }
//              Retrieve amounts
                try {
                    List<TransactionAmountOutboundDto> transactionAmountOutboundDtoList = transactionAmountService.getByTransaction(id);
                    BigDecimal voucherAmount = transactionAmountOutboundDtoList.stream()
                            .filter(a -> Objects.equals(a.getType().getCode(), AmountType.VOUCHER_AMOUNT))
                            .findFirst()
                            .map(TransactionAmountOutboundDto::getAmount)
                            .orElse(BigDecimal.ZERO);
                    voucherOutboundDto.setAmount(voucherAmount);
                } catch (Exception exception) {

                }
                for(TransactionPartnerDto partner : transactionService.getPartners(id)){
                    if(partner.getFunction().equalsIgnoreCase(PartnerFunction.CUSTOMER)){
                        try{
                            PartnerDto partnerDto = partnerService.get(partner.getPartner());
                            voucherOutboundDto.setCustomer(partnerDto);
                        }catch(PartnerNotFoundException ex){

                        }
                    }
                    if(partner.getFunction().equalsIgnoreCase(PartnerFunction.RECIPIENT)){
                        try{
                            PartnerDto partnerDto = partnerService.get(partner.getPartner());
                            voucherOutboundDto.setRecipient(partnerDto);
                        }catch(PartnerNotFoundException ex){

                        }
                    }
                }
                return voucherOutboundDto;
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<VoucherOutboundDto> search(VoucherQuery query) throws Exception {
        try {
            List<VoucherOutboundDto> voucherOutboundDtoArrayList = new ArrayList<>();
            TransactionQueryDto queryDto = new TransactionQueryDto();
            queryDto.setType(TransactionType.VOUCHER);
            queryDto.setParent(query.getParent());
            for (String id : transactionService.search(queryDto)) {
                voucherOutboundDtoArrayList.add(get(id));
            }
            return voucherOutboundDtoArrayList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean edit(VoucherEditDto voucherEditDto, String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            TransactionEntity entity = transactionRepository.getById(id);
            boolean edited = false;
            if(entity != null){
                try{
                    if (voucherEditDto.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                        TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                        transactionAmountInboundDto.setTransaction(id);
                        transactionAmountInboundDto.setAmount(voucherEditDto.getAmount());
                        transactionAmountInboundDto.setType(AmountType.VOUCHER_AMOUNT);
                        transactionAmountService.save(transactionAmountInboundDto);
                    }
                    if (voucherEditDto.getStatus() != null) {
                        transactionEditDto.setStatus(voucherEditDto.getStatus());
                        transactionService.edit(transactionEditDto);
                        entity.setStatus(voucherEditDto.getStatus());
                    }
                    if (voucherEditDto.getStatusReason() != null) {
                        transactionEditDto.setStatusReason(voucherEditDto.getStatusReason());
                        transactionService.edit(transactionEditDto);
                    }
                    if (voucherEditDto.getExpiryDate() != null) {
                        TransactionDateEdit transactionDate = new TransactionDateEdit();
                        transactionDate.setTransaction(id);
                        transactionDate.setType(DateType.EXPIRY_DATE);
                        transactionDate.setValue(voucherEditDto.getExpiryDate());
                        edited = transactionService.dateEdit(transactionDate);
                    }
                    entity.setChangedBy(getUser());
                    transactionRepository.save(entity);
                }
                catch(Exception e){
                }
            }
            return edited;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean delete(String id) {
        try {
            transactionService.delete(id);
            for (TransactionDateDto dates : transactionService.getDates(id)) {
                transactionService.removeDate(id, dates.getType());
            }
            for (TransactionPartnerDto partners : transactionService.getPartners(id)) {
                transactionService.removePartner(id, partners.getFunction(), partners.getPartner());
            }
            for (TransactionAmountDto amounts : transactionService.getAmounts(id)) {
//              transactionService.removeAmount(id, amounts.getType());
            }
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }
}
