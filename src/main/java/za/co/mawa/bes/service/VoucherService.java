package za.co.mawa.bes.service;

import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.VoucherDao;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.transaction.date.TransactionDateEditDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.voucher.*;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkPKEntity;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.*;
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
    @Autowired
    TransactionAmountRepository transactionAmountRepository;
    @Autowired
    TransactionLinkRepository transactionLinkRepository;

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
            if (voucherInboundDto.getContractId() != null) {
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
            TransactionPartnerDto partner = new TransactionPartnerDto();
            partner.setPartner(voucherInboundDto.getRecipientId());
            partner.setFunction(PartnerFunction.RECIPIENT);
            partner.setTransaction(transactionDto.getId());
            transactionService.addPartner(partner);

            TransactionDateDto dateCreate = new TransactionDateDto();
            dateCreate.setValue(new Date());
            dateCreate.setTransaction(transactionDto.getId());
            dateCreate.setType(DateType.CREATED);
            transactionService.addDate(dateCreate);

            TransactionDateDto dateDto = new TransactionDateDto();
            dateDto.setValue(voucherInboundDto.getExpiryDate());
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
            TransactionEntity entity = transactionRepository.getById(id);
            if (transactionDto != null) {
                voucherOutboundDto.setId(transactionDto.getId());
                voucherOutboundDto.setNumber(transactionDto.getNumber());
                try{
                    voucherOutboundDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, entity.getStatus()));
                }
                catch (Exception ex){
                    System.out.println(ex);
                }
                try{
                    voucherOutboundDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
                }
                catch(Exception ex){
                    System.out.println(ex);
                }
                try{
                    voucherOutboundDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
                }
                catch(Exception ex){
                    System.out.println(ex);
                }
                voucherOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
                if(entity.getChangedBy() != null){
                    voucherOutboundDto.setChangedBy(userService.getUserByPartnerId(transactionDto.getChangedBy()).getPartner());
                }
                for (TransactionDateDto dates : transactionService.getDates(id)) {
                    if (dates.getType().equalsIgnoreCase(DateType.CREATED)) {
                        voucherOutboundDto.setDateCreated(Conversion.dateToString(dates.getValue()));
                    }
                    if (dates.getType().equalsIgnoreCase(DateType.EXPIRY_DATE)) {
                        voucherOutboundDto.setExpiryDate(Conversion.dateToString(dates.getValue()));
                    }
                }
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
                for(TransactionPartnerDto partner:transactionService.getPartners(id)){
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
//                List<TransactionLinkDto> transactionLinkDto = transactionService.getLinks(id);
//                String externalContractDetails = transactionDto.getExternalContractDetails();
//                if (externalContractDetails != null) {
//                    voucherOutboundDto.setContractId(externalContractDetails);
//                }
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

    public VoucherOutboundDto edit(String id, VoucherEditDto voucherEditDto) throws Exception {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if (transactionDto == null) {
                throw new Exception("Transaction not found");
            }
            TransactionEntity entity = transactionRepository.getById(id);
            entity.setChangedBy(UserContext.getCurrentUserPartner());
            if(voucherEditDto.getType() != null){
                entity.setStatus(transactionDto.getStatus());
                transactionRepository.save(entity);
            }
            if (voucherEditDto.getAmount() != null) {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(voucherEditDto.getAmount());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.VOUCHER_AMOUNT);
                transactionAmountService.save(transactionAmountInboundDto);
            }
            Boolean edited = false;
            if(voucherEditDto.getExpiryDate() != null){
                TransactionDateEdit dateEdit = new TransactionDateEdit();
                dateEdit.setType(DateType.EXPIRY_DATE);
                dateEdit.setValue(voucherEditDto.getExpiryDate());
                dateEdit.setTransaction(id);
                edited = transactionService.dateEdit(dateEdit);
            }
            TransactionLinkDto dto = new TransactionLinkDto();
            dto.setTransaction1(voucherEditDto.getContractId());
            dto.setTransaction2(id);
            dto.setType(TransactionType.VOUCHER);
            transactionService.addLink(dto);

            return get(transactionDto.getId());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
    public boolean edit(VoucherEditDto voucherEditDto, String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            boolean edited = false;

            if (voucherEditDto.getAmount().compareTo(BigDecimal.ZERO) != 0) {

                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(voucherEditDto.getAmount());
                transactionAmountService.save(transactionAmountInboundDto);
            }
            if (voucherEditDto.getStatus() != null) {
                transactionEditDto.setStatus(voucherEditDto.getStatus());
                transactionService.edit(transactionEditDto);
            }
            if (voucherEditDto.getStatusReason() != null) {
                transactionEditDto.setStatusReason(voucherEditDto.getStatusReason());
                transactionService.edit(transactionEditDto);
            }
            if (voucherEditDto.getExpiryDate() != null) {
                TransactionDateEdit transactionDate = new TransactionDateEdit();
                transactionDate.setTransaction(id);
                transactionDate.setType(DateType.EXPIRY_DATE);
                transactionDate.setValue(Conversion.stringToDate(String.valueOf(voucherEditDto.getExpiryDate())));
                edited = transactionService.dateEdit(transactionDate);
            }
            if (voucherEditDto.getAmount().compareTo(BigDecimal.ZERO) != 0) {
//                edited = transactionService.editAmount(PriceType.VOUCHER_AMOUNT,edit.getAmount(),id);
            }
            return edited;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public VoucherOutboundDto edit(String id, VoucherEditDto voucherEditDto) throws Exception {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            TransactionEntity entity = transactionRepository.getById(id);
            if (transactionDto == null) {
                throw new Exception("Transaction not found");
            }
            if(voucherEditDto.getStatus() != null){
                entity.setStatus(voucherEditDto.getStatus());
            }
            entity.setChangedBy(UserContext.getCurrentUserPartner());
            transactionRepository.save(entity);
            if (voucherEditDto.getAmount() != null) {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(voucherEditDto.getAmount());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.VOUCHER_AMOUNT);
                transactionAmountService.save(transactionAmountInboundDto);
            }
            Boolean edited = false;
            if(voucherEditDto.getExpiryDate() != null){
                TransactionDateEdit dateEdit = new TransactionDateEdit();
                dateEdit.setType(DateType.EXPIRY_DATE);
                dateEdit.setValue(Conversion.stringToDate(String.valueOf(voucherEditDto.getExpiryDate())));
                dateEdit.setTransaction(id);
                edited = transactionService.dateEdit(dateEdit);
            }
            System.out.println(edited);


            return get(transactionDto.getId());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
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
}
