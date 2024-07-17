package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.ClaimCancelDto;
import za.co.mawa.bes.dto.ClaimDisputeDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.claim.ClaimCreateDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.claim.ClaimOutboundDto;
import za.co.mawa.bes.dto.claim.ClaimQueryDto;
import za.co.mawa.bes.dto.comment.CommentDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.dto.transaction.link.TransactionLinkOutboundDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.transaction.text.TransactionTextDto;
import za.co.mawa.bes.dto.voucher.VoucherCreateDto;
import za.co.mawa.bes.dto.voucher.VoucherInboundDto;
import za.co.mawa.bes.entity.FieldOptionEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.*;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ClaimService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    CommentService commentService;
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
    @Autowired
    TransactionLinkService transactionLinkService;


    List<String> voucherClaimTypeList = Arrays.asList("FUNERAL", "GROUP-FUNERAL");
    List<String> autoApprovalTypeList = new ArrayList<>();

    public ClaimOutboundDto create(ClaimCreateDto claimCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.CLAIM);
            transactionCreateDto.setSubType(claimCreateDto.getType());
            transactionCreateDto.setLocation(claimCreateDto.getBranch());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            ClaimOutboundDto claimOutboundDto = new ClaimOutboundDto();
            claimOutboundDto.setId(transactionDto.getId());
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
            if (claimCreateDto.getMemberId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(claimCreateDto.getMemberId());
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
            if (claimCreateDto.getInformantId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.INFORMANT);
                transactionPartnerDto.setPartner(claimCreateDto.getInformantId());
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
            if (claimCreateDto.getContractId() != null) {
                TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                transactionLinkDto.setTransaction1(claimCreateDto.getContractId());
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
                account.setTransaction(claimOutboundDto.getId());
                account.setAccountNumber(claimCreateDto.getBankAccount().getAccountNumber());
                account.setBankName(claimCreateDto.getBankAccount().getBankName());
                account.setBranchCode(claimCreateDto.getBankAccount().getBranchCode());
                account.setAccountType(claimCreateDto.getBankAccount().getAccountType());
                transactionService.addBankAccount(account);
            }
            List<TransactionAmountOutboundDto> transactionAmountOutboundDtoList = transactionAmountService.getByTransaction(claimCreateDto.getMembershipId());
            Iterator iterator = transactionAmountOutboundDtoList.stream()
                    .filter(a -> Objects.equals(a.getType().getCode(), AmountType.SERVICE_AMOUNT))
                    .toList().iterator();
            if (iterator.hasNext()) {
                TransactionAmountOutboundDto transactionAmountOutboundDto = (TransactionAmountOutboundDto) iterator.next();
                try {
                    TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                    transactionAmountInboundDto.setAmount(transactionAmountOutboundDto.getAmount());
                    transactionAmountInboundDto.setTransaction(claimOutboundDto.getId());
                    transactionAmountInboundDto.setType(AmountType.SERVICE_AMOUNT);
                    transactionAmountService.save(transactionAmountInboundDto);
                } catch (Exception exception) {

                }
            }
            return get(claimOutboundDto.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClaimOutboundDto get(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            ClaimOutboundDto claimOutboundDto = new ClaimOutboundDto();
            claimOutboundDto.setId(transactionDto.getId());
            claimOutboundDto.setNumber(transactionDto.getNumber());
            claimOutboundDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            claimOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            claimOutboundDto.setType(fieldOptionService.getFieldOption(Field.CLAIM_TYPE, transactionDto.getSubType()));
            claimOutboundDto.setBranch(fieldOptionService.getFieldOption(Field.BRANCH, transactionDto.getLocation()));
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(transactionDto.getId());
            transactionAttributeDto.setAttribute(TransactionAttribute.PAYMENT_METHOD);
            claimOutboundDto.setPaymentMethod(fieldOptionService.getFieldOption(Field.PAYMENT_METHOD, transactionAttributeService.get(transactionAttributeDto)));
            if (transactionLinkService.getParent(id).iterator().hasNext()) {
                TransactionLinkOutboundDto transactionLinkOutboundDto = transactionLinkService.getParent(id).iterator().next();
                claimOutboundDto.setMembership(membershipService.get(transactionLinkOutboundDto.getParent()));
                claimOutboundDto.setParent(transactionService.get(transactionLinkOutboundDto.getParent()));
            }

            try {
                for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CUSTOMER)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            claimOutboundDto.setCustomer((partnerService.get(transactionPartnerDto.getPartner())));
                            claimOutboundDto.setMember((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.DECEASED)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            claimOutboundDto.setDeceased((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CLAIMANT)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            claimOutboundDto.setClaimant((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.INFORMANT)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            claimOutboundDto.setInformant((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                }
            } catch (Exception e) {
//                throw new RuntimeException(e);
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (Objects.equals(transactionDateDto.getType(), DateType.CREATED)) {
                    claimOutboundDto.setCreationDate(transactionDateDto.getValue());
                }
                if (Objects.equals(transactionDateDto.getType(), DateType.BURIAL_DATE)) {
                    claimOutboundDto.setBurialDate(transactionDateDto.getValue());
                }
                if (Objects.equals(transactionDateDto.getType(), DateType.DEATH_DATE)) {
                    claimOutboundDto.setDeathDate(transactionDateDto.getValue());
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
                claimOutboundDto.setBankDetails(bankAccountDto);
            }
            List<TransactionLinkDto> links = transactionService.getLinks(id);
            List<CommentDto> comments = new ArrayList<>();
            for (TransactionLinkDto link : links) {

                CommentDto comment = new CommentDto();
                comment = commentService.get(link.getTransaction2());
                if(Objects.equals(comment.getType(), "COMMENT")) {
                    comments.add(comment);
                }
            }
            claimOutboundDto.setComments(comments);
            return claimOutboundDto;
        } catch (TransactionNotFound exception) {
            throw new RuntimeException(new TransactionNotFound("Claim not found"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<ClaimOutboundDto> search(ClaimQueryDto claimQueryDto) {
        List<ClaimOutboundDto> claimOutboundDtoList = new ArrayList<>();
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
            if (claimQueryDto.getInformant() != null & claimQueryDto.getInformant() != "") {
                transactionQueryDto.setPartnerNo(claimQueryDto.getInformant());
                transactionQueryDto.setPartnerFunction(PartnerFunction.INFORMANT);
            }
            if (claimQueryDto.getDeceased() != null && claimQueryDto.getDeceased() != "") {
                transactionQueryDto.setPartnerNo(claimQueryDto.getDeceased());
                transactionQueryDto.setPartnerFunction(PartnerFunction.DECEASED);
            }
            if (claimQueryDto.getMember() != null && claimQueryDto.getMember() != "") {
                transactionQueryDto.setPartnerNo(claimQueryDto.getMember());
                transactionQueryDto.setPartnerFunction(PartnerFunction.CUSTOMER);
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
            if (claimQueryDto.getParent() != null && claimQueryDto.getParent() != "") {
                transactionQueryDto.setParent(claimQueryDto.getParent());
            }
            transactionQueryDto.setType(TransactionType.CLAIM);
            for (String id : transactionService.search(transactionQueryDto)) {
                try {
                    claimOutboundDtoList.add(get(id));
                } catch (Exception exception) {
                }
            }
        } catch (Exception exception) {
        }
        return claimOutboundDtoList;
    }

    public void submit(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if (autoApprovalTypeList.contains(transactionDto.getSubType())) {
                TransactionEditDto transactionEditDto = new TransactionEditDto();
                transactionEditDto.setId(id);
                transactionEditDto.setStatus(ClaimStatus.APPROVED);
                transactionService.edit(transactionEditDto);
                approve(id);
            }else{
                TransactionEditDto transactionEditDto = new TransactionEditDto();
                transactionEditDto.setId(id);
                transactionEditDto.setStatus(ClaimStatus.AWAITING_APPROVAL);
                transactionService.edit(transactionEditDto);
            }
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
            if (voucherClaimTypeList.contains(transactionDto.getSubType())) {
                VoucherInboundDto voucherInboundDto = new VoucherInboundDto();
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
                voucherInboundDto.setContractId(id);
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
