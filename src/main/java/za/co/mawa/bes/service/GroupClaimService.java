package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.ClaimCancelDto;
import za.co.mawa.bes.dto.ClaimDisputeDto;
import za.co.mawa.bes.dto.claim.ClaimCreateDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.claim.ClaimQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.transaction.text.TransactionTextDto;
import za.co.mawa.bes.dto.voucher.VoucherCreateDto;
import za.co.mawa.bes.dto.voucher.VoucherInboundDto;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
public class GroupClaimService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    TransactionTextService transactionTextService;
    @Autowired
    TransactionAttributeService transactionAttributeService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    MembershipService membershipService;
    @Autowired
    TransactionBankAccountService transactionBankAccountService;
    @Autowired
    VoucherService voucherService;
    @Autowired
    TransactionAmountService transactionAmountService;

    public ClaimDto create(ClaimCreateDto claimCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.CLAIM);
            transactionCreateDto.setSubType(claimCreateDto.getType());
            transactionCreateDto.setLocation(claimCreateDto.getBranch());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            ClaimDto claimDto = new ClaimDto();
            claimDto.setId(transactionDto.getId());
            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            transactionService.addDate(creationDate);
            if (claimCreateDto.getPaymentMethod() != null) {
                TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
                transactionAttributeDto.setTransaction(transactionDto.getId());
                transactionAttributeDto.setAttribute(TransactionAttribute.PAYMENT_METHOD);
                transactionAttributeDto.setValue(claimCreateDto.getPaymentMethod());
                transactionAttributeService.add(transactionAttributeDto);
            }

            if (claimCreateDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(claimCreateDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (claimCreateDto.getDeceasedId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.DECEASED);
                transactionPartnerDto.setPartner(claimCreateDto.getDeceasedId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (claimCreateDto.getClaimantId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CLAIMANT);
                transactionPartnerDto.setPartner(claimCreateDto.getClaimantId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (claimCreateDto.getMembershipId() != null) {
                TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                transactionLinkDto.setTransaction1(claimCreateDto.getMembershipId());
                transactionLinkDto.setTransaction2(transactionDto.getId());
                transactionLinkDto.setType(TransactionType.CLAIM);
                transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                transactionService.addLink(transactionLinkDto);
            }
            if (claimCreateDto.getDeathDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.DEATH_DATE);
                transactionDateDto.setValue(claimCreateDto.getDeathDate());
                transactionService.addDate(transactionDateDto);
            }
            if (claimCreateDto.getBurialDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.BURIAL_DATE);
                transactionDateDto.setValue(claimCreateDto.getBurialDate());
                transactionService.addDate(transactionDateDto);
            }
            if (claimCreateDto.getBankAccount() != null) {
                TransactionAccountDto account = new TransactionAccountDto();
                account.setAccountHolder(claimCreateDto.getBankAccount().getAccountHolder());
                account.setTransaction(claimDto.getId());
                account.setAccountNumber(claimCreateDto.getBankAccount().getAccountNumber());
                account.setBankName(claimCreateDto.getBankAccount().getBankName());
                account.setBranchCode(claimCreateDto.getBankAccount().getBranchCode());
                account.setAccountType(claimCreateDto.getBankAccount().getAccountType());
                transactionService.addBankAccount(account);
            }
            return claimDto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClaimDto get(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            ClaimDto claimDto = new ClaimDto();
            claimDto.setId(transactionDto.getId());
            claimDto.setNumber(transactionDto.getNumber());
            claimDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            claimDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            claimDto.setType(fieldOptionService.getFieldOption(Field.CLAIM_TYPE, transactionDto.getSubType()));
            claimDto.setBranch(fieldOptionService.getFieldOption(Field.BRANCH, transactionDto.getLocation()));
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(transactionDto.getId());
            transactionAttributeDto.setAttribute(TransactionAttribute.PAYMENT_METHOD);
            claimDto.setPaymentMethod(fieldOptionService.getFieldOption(Field.PAYMENT_METHOD, transactionAttributeService.get(transactionAttributeDto)));

            TransactionLinkEntity transactionLink = transactionService.getTransaction(TransactionType.CLAIM, id);
            if (transactionLink != null) {
                claimDto.setMembership(membershipService.get(transactionLink.getTransactionLinkPKEntity().getTransaction1()));
            }
            try {
                for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CUSTOMER)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            claimDto.setMember((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.DECEASED)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            claimDto.setDeceased((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CLAIMANT)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            claimDto.setClaimant((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                }
            } catch (Exception e) {
//                throw new RuntimeException(e);
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (Objects.equals(transactionDateDto.getType(), DateType.CREATED)) {
                    claimDto.setCreationDate(transactionDateDto.getValue());
                }
                if (Objects.equals(transactionDateDto.getType(), DateType.BURIAL_DATE)) {
                    claimDto.setBurialDate(transactionDateDto.getValue());
                }
                if (Objects.equals(transactionDateDto.getType(), DateType.DEATH_DATE)) {
                    claimDto.setDeathDate(transactionDateDto.getValue());
                }
            }
            TransactionBankAccountDto transactionBankAccountDto = transactionBankAccountService.get(id);
            if (transactionBankAccountDto != null) {
                BankAccountDto bankAccountDto = new BankAccountDto();
                bankAccountDto.setAccountHolder(transactionBankAccountDto.getAccountHolder());
                bankAccountDto.setBankName(transactionBankAccountDto.getBankName());
                bankAccountDto.setBranchCode(transactionBankAccountDto.getBranchCode());
                bankAccountDto.setAccountType(transactionBankAccountDto.getAccountType());
                bankAccountDto.setAccountNumber(transactionBankAccountDto.getAccountNumber());
                claimDto.setBankDetails(bankAccountDto);
            }
            return claimDto;
        } catch (TransactionNotFound exception) {
            throw new RuntimeException(new TransactionNotFound("Claim not found"));
        }
    }

    public List<ClaimDto> search(ClaimQueryDto claimQueryDto) {
        List<ClaimDto> claimDtoList = new ArrayList<>();
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            if (claimQueryDto.getStatus() != null && claimQueryDto.getStatus() != "") {
                transactionQueryDto.setStatus(claimQueryDto.getStatus());
            }
            if (claimQueryDto.getNumber() != null && claimQueryDto.getNumber() != "") {
                transactionQueryDto.setNumber(claimQueryDto.getNumber());
            }
            if (claimQueryDto.getClaimant() != null & claimQueryDto.getClaimant() != "") {
                transactionQueryDto.setPartnerNo(claimQueryDto.getClaimant());
                transactionQueryDto.setPartnerFunction(PartnerFunction.CLAIMANT);
            }
            if (claimQueryDto.getDeceased() != null && claimQueryDto.getDeceased() != "") {
                transactionQueryDto.setPartnerNo(claimQueryDto.getDeceased());
                transactionQueryDto.setPartnerFunction(PartnerFunction.DECEASED);
            }
            if (claimQueryDto.getCustomer() != null && claimQueryDto.getCustomer() != "") {
                transactionQueryDto.setPartnerNo(claimQueryDto.getCustomer());
                transactionQueryDto.setPartnerFunction(PartnerFunction.CUSTOMER);
            }
            if (claimQueryDto.getType() != null && claimQueryDto.getType() != "") {
                transactionQueryDto.setSubtype(claimQueryDto.getType());
            }
            if (claimQueryDto.getDeathDate() != null) {
                transactionQueryDto.setValue(claimQueryDto.getDeathDate());
                transactionQueryDto.setDateType(DateType.DEATH_DATE);
            }
            if (claimQueryDto.getBurialDate() != null) {
                transactionQueryDto.setValue(claimQueryDto.getBurialDate());
                transactionQueryDto.setDateType(DateType.BURIAL_DATE);
            }
            if (claimQueryDto.getMembership() != null && claimQueryDto.getMembership() != "") {
                transactionQueryDto.setTransactionlink(claimQueryDto.getMembership());
            }
            transactionQueryDto.setType(TransactionType.CLAIM);
            for (String id : transactionService.search(transactionQueryDto)) {
                try {
                    claimDtoList.add(get(id));
                } catch (Exception exception) {
                }
            }
        } catch (Exception exception) {
        }
        return claimDtoList;
    }

    public void submit(String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(ClaimStatus.AWAITING_APPROVAL);
            transactionService.edit(transactionEditDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void approve(String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(ClaimStatus.APPROVED);
            transactionService.edit(transactionEditDto);
            TransactionDto transactionDto = transactionService.get(id);
            if (transactionDto.getType() == "GROUP-SOCIETY-FUNERAL") {
                VoucherInboundDto voucherInboundDto = new VoucherInboundDto();
                voucherInboundDto.setType(transactionDto.getType());
                List<TransactionAmountOutboundDto> transactionAmountOutboundDtoList = transactionAmountService.getByTransaction(id);
                Iterator iterator = transactionAmountOutboundDtoList.stream()
                        .filter(a -> Objects.equals(a.getType().getCode(), AmountType.SERVICE_AMOUNT))
                        .toList().iterator();
                if (iterator.hasNext()) {
                    TransactionAmountOutboundDto transactionAmountOutboundDto = (TransactionAmountOutboundDto) iterator.next();
                    voucherInboundDto.setAmount(transactionAmountOutboundDto.getAmount());
                }
                List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id);
                Iterator partnerIterator = transactionPartnerDtoList.stream()
                        .filter(a -> Objects.equals(a.getFunction(), PartnerFunction.CUSTOMER))
                        .toList().iterator();
                if (partnerIterator.hasNext()) {
                    TransactionPartnerDto transactionPartnerDto = (TransactionPartnerDto) partnerIterator.next();
                    voucherInboundDto.setRecipientId(transactionPartnerDto.getPartner());
                }
                voucherService.create(voucherInboundDto);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void dispute(ClaimDisputeDto claimDisputeDto) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(claimDisputeDto.getClaimId());
            transactionEditDto.setStatus(ClaimStatus.DISPUTED);
            transactionEditDto.setStatusReason(claimDisputeDto.getReason());
            transactionService.edit(transactionEditDto);
            TransactionTextDto transactionTextDto = new TransactionTextDto();
            transactionTextDto.setTransaction(claimDisputeDto.getClaimId());
            transactionTextDto.setType(TextType.CLAIM_DISPUTE);
            transactionTextDto.setText(claimDisputeDto.getComments());
            transactionTextService.add(transactionTextDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel(ClaimCancelDto claimCancelDto) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(claimCancelDto.getClaimId());
            transactionEditDto.setStatus(ClaimStatus.CANCELLED);
            transactionEditDto.setStatusReason(claimCancelDto.getReason());
            transactionService.edit(transactionEditDto);
            TransactionTextDto transactionTextDto = new TransactionTextDto();
            transactionTextDto.setTransaction(claimCancelDto.getClaimId());
            transactionTextDto.setType(TextType.CLAIM_CANCEL);
            transactionTextDto.setText(claimCancelDto.getComments());
            transactionTextService.add(transactionTextDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
