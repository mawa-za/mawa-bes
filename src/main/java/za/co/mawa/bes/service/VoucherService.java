package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.VoucherDao;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.voucher.VoucherCreateDto;
import za.co.mawa.bes.dto.voucher.VoucherDto;
import za.co.mawa.bes.dto.voucher.VoucherEditDto;
import za.co.mawa.bes.dto.voucher.VoucherQuery;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

@Service
public class VoucherService implements VoucherDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;

    @Override
    public String create(VoucherCreateDto createDto) throws Exception {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.VOUCHER);
            transactionCreateDto.setSubType(createDto.getType());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            try {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(createDto.getAmount());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.VOUCHER_AMOUNT);
                transactionAmountService.save(transactionAmountInboundDto);
            } catch (Exception exception) {

            }

            TransactionPartnerDto partner = new TransactionPartnerDto();
            partner.setPartner(createDto.getCustomerId());
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
            if (createDto.getExpiryDate() != null && createDto.getExpiryDate() != "") {
                dateDto.setValue(Conversion.stringToDate(createDto.getExpiryDate()));
            }
            dateDto.setTransaction(transactionDto.getId());
            dateDto.setType(DateType.EXPIRY_DATE);
            transactionService.addDate(dateDto);

            return transactionDto.getId();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

    }

    @Override
    public VoucherDto get(String id) throws Exception {
        try {
            VoucherDto voucher = new VoucherDto();
            TransactionDto transactionDto = transactionService.get(id);
            if (transactionDto != null) {
                voucher.setId(transactionDto.getId());
                voucher.setNumber(transactionDto.getNumber());
                voucher.setStatus(transactionDto.getStatus());
                voucher.setType(transactionDto.getSubType());
                voucher.setStatusReason(transactionDto.getStatusReason());
                voucher.setCreatedBy(transactionDto.getCreatedBy());
                voucher.setChangedBy(transactionDto.getChangedBy());
                for (TransactionDateDto dates : transactionService.getDates(id)) {
                    if (dates.getType().equalsIgnoreCase(DateType.CREATED)) {
                        voucher.setDateCreated(Conversion.dateToString(dates.getValue()));
                    }
                    if (dates.getType().equalsIgnoreCase(DateType.EXPIRY_DATE)) {
                        voucher.setExpiryDate(Conversion.dateToString(dates.getValue()));
                    }
                }
                try {
                    BigDecimal voucherAmount = transactionAmountService.getByTransaction(id).stream()
                            .filter(a -> Objects.equals(a.getType().getCode(), AmountType.VOUCHER_AMOUNT))
                            .toList().iterator().next().getAmount();
                    voucher.setAmount(voucherAmount);
                } catch (Exception exception) {

                }

                for (TransactionPartnerDto partner : transactionService.getPartners(id)) {
                    if (partner.getFunction().equalsIgnoreCase(PartnerFunction.RECIPIENT)) {
                        PartnerDto partnerDetails = partnerService.getOptional(partner.getPartner());
                        if (partnerDetails != null) {
                            voucher.setCustomer(partnerDetails);
                        }
                        break;
                    }
                }
                return voucher;
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ArrayList<VoucherDto> search(VoucherQuery query) throws Exception {
        try {
            ArrayList<VoucherDto> voucherDtos = new ArrayList<>();

            if (query.getIdNumber() != null && query.getIdNumber() != "") {
                for (PartnerIdentityEntity identityEntity : partnerIdentityRepository.findPartnerIdentityByValue(query.getIdNumber())) {
                    TransactionQueryDto queryDto = new TransactionQueryDto();
                    queryDto.setType(TransactionType.VOUCHER);
                    queryDto.setPartnerNo(identityEntity.getPartner());
                    queryDto.setPartnerFunction(PartnerFunction.RECIPIENT);
                    for (String id : transactionService.search(queryDto)) {
                        VoucherDto voucher = new VoucherDto();
                        voucher = get(id);
                        voucherDtos.add(voucher);
                    }
                }
            } else {
                TransactionQueryDto queryDto = new TransactionQueryDto();
                queryDto.setType(TransactionType.VOUCHER);
                if (query.getExpiryDate() != null && query.getExpiryDate() != "") {
                    queryDto.setValue(Conversion.stringToDate(query.getExpiryDate()));
                    queryDto.setDateType(DateType.EXPIRY_DATE);
                }
                if (query.getDateCreated() != null && query.getDateCreated() != "") {
                    queryDto.setValue(Conversion.stringToDate(query.getDateCreated()));
                    queryDto.setDateType(DateType.CREATED);
                }
                if (query.getStatus() != null && query.getStatus() != "") {
                    queryDto.setStatus(query.getStatus());
                }
                if (query.getNumber() != null && query.getNumber() != "") {
                    queryDto.setNumber(query.getNumber());
                }
                if (query.getCustomerId() != null && query.getCustomerId() != "") {
                    queryDto.setPartnerNo(query.getCustomerId());
                    queryDto.setPartnerFunction(PartnerFunction.RECIPIENT);
                }
                for (String id : transactionService.search(queryDto)) {
                    VoucherDto voucher = new VoucherDto();
                    voucher = get(id);
                    voucherDtos.add(voucher);
                }
            }
            return voucherDtos;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean edit(VoucherEditDto edit, String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            boolean edited = false;
            if (edit.getStatusReason() != null && edit.getStatusReason() != "") {
                transactionEditDto.setStatusReason(edit.getStatusReason());
            }
            if (edit.getStatus() != null && edit.getStatus() != "") {
                transactionEditDto.setStatus(edit.getStatus());
            }
            if (transactionEditDto != null) {
                transactionEditDto.setId(id);
                transactionService.edit(transactionEditDto);
            }
            if (edit.getExpiryDate() != null && edit.getExpiryDate() != "") {
                TransactionDateEdit transactionDate = new TransactionDateEdit();
                transactionDate.setTransaction(id);
                transactionDate.setType(DateType.EXPIRY_DATE);
                transactionDate.setValue(Conversion.stringToDate(edit.getExpiryDate()));
                edited = transactionService.dateEdit(transactionDate);
            }
            if (edit.getAmount().compareTo(BigDecimal.ZERO) != 0) {
//                edited = transactionService.editAmount(PriceType.VOUCHER_AMOUNT,edit.getAmount(),id);
            }
            return edited;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
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
