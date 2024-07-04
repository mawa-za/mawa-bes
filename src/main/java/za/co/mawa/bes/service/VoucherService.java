package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.VoucherDao;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.voucher.*;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
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
            voucherOutboundDto.setStatus(transactionDto.getStatus());
            voucherOutboundDto.setType(transactionDto.getSubType());
            voucherOutboundDto.setStatusReason(transactionDto.getStatusReason());
            voucherOutboundDto.setCreatedBy(transactionDto.getCreatedBy());
            voucherOutboundDto.setChangedBy(transactionDto.getChangedBy());

            // Retrieve dates
            for (TransactionDateDto dates : transactionService.getDates(id)) {
                if (dates.getType().equalsIgnoreCase(DateType.CREATED)) {
                    voucherOutboundDto.setDateCreated(Conversion.dateToString(dates.getValue()));
                }
                if (dates.getType().equalsIgnoreCase(DateType.EXPIRY_DATE)) {
                    voucherOutboundDto.setExpiryDate(Conversion.dateToString(dates.getValue()));
                }
            }

            // Retrieve amounts
            try {
                List<TransactionAmountOutboundDto> transactionAmountOutboundDtoList = transactionAmountService.getByTransaction(id);
                BigDecimal voucherAmount = transactionAmountOutboundDtoList.stream()
                        .filter(a -> Objects.equals(a.getType().getCode(), AmountType.VOUCHER_AMOUNT))
                        .findFirst()
                        .map(TransactionAmountOutboundDto::getAmount)
                        .orElse(BigDecimal.ZERO);
                voucherOutboundDto.setAmount(voucherAmount);
            } catch (Exception exception) {
                // Handle exception
            }

            // Retrieve partners
            for (TransactionPartnerDto partner : transactionService.getPartners(id)) {
                if (partner.getFunction().equalsIgnoreCase(PartnerFunction.RECIPIENT)) {
                    PartnerDto partnerDetails = partnerService.getOptional(partner.getPartner());
                    if (partnerDetails != null) {
                        voucherOutboundDto.setRecipient(partnerDetails);
                    }
                    break;
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
            List<VoucherOutboundDto> voucherOutboundDtoList = new ArrayList<>();
            TransactionQueryDto queryDto = new TransactionQueryDto();
            queryDto.setType(TransactionType.VOUCHER);
            queryDto.setParent(query.getParent());
            for (String id : transactionService.search(queryDto)) {
                voucherOutboundDtoList.add(get(id));
            }
            return voucherOutboundDtoList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean edit(VoucherInboundDto voucherInboundDto) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            boolean edited = false;
//            if (voucherInboundDto.getStatusReason() != null && voucherInboundDto.getStatusReason() != "") {
//                transactionEditDto.setStatusReason(voucherInboundDto.getStatusReason());
//            }
//            if (voucherInboundDto.getStatus() != null && voucherInboundDto.getStatus() != "") {
//                transactionEditDto.setStatus(voucherInboundDto.getStatus());
//            }
//            if (transactionEditDto != null) {
//                transactionEditDto.setId(id);
//                transactionService.edit(transactionEditDto);
//            }
//            if (voucherInboundDto.getExpiryDate() != null && voucherInboundDto.getExpiryDate() != "") {
//                TransactionDateEdit transactionDate = new TransactionDateEdit();
//                transactionDate.setTransaction(id);
//                transactionDate.setType(DateType.EXPIRY_DATE);
//                transactionDate.setValue(Conversion.stringToDate(voucherInboundDto.getExpiryDate()));
//                edited = transactionService.dateEdit(transactionDate);
//            }
//            if (voucherInboundDto.getAmount().compareTo(BigDecimal.ZERO) != 0) {
////                edited = transactionService.editAmount(PriceType.VOUCHER_AMOUNT,edit.getAmount(),id);
//            }
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
}
