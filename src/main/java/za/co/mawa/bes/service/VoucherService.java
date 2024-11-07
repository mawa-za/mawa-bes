package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.VoucherDao;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountEditDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.transaction.date.TransactionDateEditDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.voucher.*;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;

import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkPKEntity;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.*;

import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.*;

@Service
public class VoucherService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    UserService userService;
    @Autowired
    FieldOptionService fieldOptionService;
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
            transactionCreateDto.setStatusReason(voucherInboundDto.getStatusReason());
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
                try{
                    voucherOutboundDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
                }
                catch (Exception ex){
                }
                try{
                    voucherOutboundDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
                }
                catch(Exception ex){
                }
                try{
                    voucherOutboundDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
                }
                catch(Exception ex){
                }
                try{
                    voucherOutboundDto.setChangedBy(userService.getUserByPartnerId(transactionDto.getChangedBy()).getPartner());
                }
                catch(Exception ex){
                }
                voucherOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));

                for (TransactionDateDto dates : transactionService.getDates(id)) {
                    if (Objects.equals(dates.getType(), DateType.CREATED)) {
                        voucherOutboundDto.setDateCreated(Conversion.stringToDate(String.valueOf(dates.getValue())));
                    }
                    if (Objects.equals(dates.getType(), DateType.EXPIRY_DATE)) {
                        voucherOutboundDto.setExpiryDate(Conversion.stringToDate(String.valueOf(dates.getValue())));
                    }
                }
                for(TransactionPartnerDto partner:transactionService.getPartners(id)){
                    if(partner.getFunction().equalsIgnoreCase(PartnerFunction.CUSTOMER)){
                        PartnerDto customer =  partnerService.getOptional(partner.getPartner());
                        if(customer != null){
                            voucherOutboundDto.setCustomer(customer);
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
                try {
                    BigDecimal voucherAmount = transactionAmountService.getByTransaction(id).stream()
                            .filter(a -> Objects.equals(a.getType().getCode(), AmountType.VOUCHER_AMOUNT))
                            .toList().iterator().next().getAmount();
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
                List<TransactionLinkDto> links = transactionService.getLinks(id);
                for (TransactionLinkDto link : links) {
                    voucherOutboundDto.setContract(transactionService.get(link.getTransaction2()));
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

    public VoucherOutboundDto edit(String id, VoucherEditDto voucherEditDto) throws Exception {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if (transactionDto == null) {
                throw new Exception("Transaction not found");
            }
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);

            if (voucherEditDto.getStatus() != null && !voucherEditDto.getStatus().isEmpty()) {
                transactionEditDto.setStatus(voucherEditDto.getStatus());
            }
            if (voucherEditDto.getStatusReason() != null) {
                transactionEditDto.setStatusReason(voucherEditDto.getStatusReason());
            }
            if (voucherEditDto.getType() != null) {
                transactionEditDto.setType(voucherEditDto.getType());
            }
            if(voucherEditDto.getAmount() != null && voucherEditDto.getAmount().compareTo(BigDecimal.ZERO) != 0){
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(voucherEditDto.getAmount());
                transactionAmountService.save(transactionAmountInboundDto);
            }
            if (voucherEditDto.getExpiryDate() != null) {
                TransactionDateEdit dateEdit = new TransactionDateEdit();
                dateEdit.setType(DateType.EXPIRY_DATE);
                dateEdit.setValue(voucherEditDto.getExpiryDate());
                dateEdit.setTransaction(id);
                transactionService.dateEdit(dateEdit);
            }
            transactionService.edit(transactionEditDto);
            if (voucherEditDto.getContractId() != null && !voucherEditDto.getContractId().isEmpty()) {
                TransactionLinkDto dto = new TransactionLinkDto();
                dto.setTransaction1(voucherEditDto.getContractId());
                dto.setTransaction2(id);
                dto.setType(TransactionType.VOUCHER);
                transactionService.addLink(dto);
            }
            return get(transactionDto.getId());

        } catch (Exception exception) {
            throw new RuntimeException("Failed to edit transaction: " + exception.getMessage(), exception);
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
                transactionService.removeAmount(amounts.getId());
            }
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
