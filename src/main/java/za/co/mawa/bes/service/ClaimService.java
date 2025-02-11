package za.co.mawa.bes.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.ClaimCancelDto;
import za.co.mawa.bes.dto.ClaimDisputeDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.claim.*;
import za.co.mawa.bes.dto.comment.CommentDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountOutboundDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.link.TransactionLinkOutboundDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.transaction.text.TransactionTextDto;
import za.co.mawa.bes.dto.voucher.VoucherCreateDto;
import za.co.mawa.bes.dto.voucher.VoucherInboundDto;
import za.co.mawa.bes.entity.FieldOptionEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.TransactionAmountRepository;
import za.co.mawa.bes.repository.TransactionViewRepository;
import za.co.mawa.bes.utils.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
    @Autowired
    TransactionViewRepository transactionViewRepository;
    @Autowired
    TransactionAmountRepository transactionAmountRepository;
    @Autowired
    PartnerRepository partnerRepository;


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
                //get the deceased and set the status to deceased
                try{
                    PartnerEntity deceased = partnerRepository.getById(claimCreateDto.getDeceasedId());
                    deceased.setStatus(Status.DECEASED);
                    partnerRepository.save(deceased);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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

            try {
                claimOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason().toUpperCase()));
            }catch (Exception e){}

            if(transactionDto.getDescription() == null ){
                claimOutboundDto.setDescription(transactionDto.getSubDescription());
            }else {
                claimOutboundDto.setDescription(transactionDto.getDescription());
            }
          
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
            try{
                List<TransactionAmountEntity> transactionAmountEntities = transactionAmountRepository.getByTransaction(id);
                for(TransactionAmountEntity transactionAmount : transactionAmountEntities){
                    if(transactionAmount.getType().equals(AmountType.PAID_OUT_AMOUNT)){
                        TransactionAmountOutboundDto transactionAmountOutboundDto = new TransactionAmountOutboundDto();
                        transactionAmountOutboundDto.setId(transactionAmount.getId());
                        transactionAmountOutboundDto.setTransaction(id);
                        transactionAmountOutboundDto.setAmount(transactionAmount.getAmount());
                        transactionAmountOutboundDto.setChangedBy(transactionAmount.getChangedBy());
                        transactionAmountOutboundDto.setCreatedBy(transactionAmount.getCreatedBy());
                        claimOutboundDto.setPaidOutAmount(transactionAmountOutboundDto);
                    }
                }
            }catch (Exception e){}

            try {
                List<TransactionLinkDto> links = transactionService.getLinks(id);
                List<CommentDto> comments = new ArrayList<>();
                for (TransactionLinkDto link : links) {

                    CommentDto comment = new CommentDto();
                    comment = commentService.get(link.getTransaction2());
                    if (Objects.equals(comment.getType(), "COMMENT")) {
                        comments.add(comment);
                    }
                }
                claimOutboundDto.setComments(comments);

            }catch (Exception e){}

            return claimOutboundDto;
        } catch (TransactionNotFound exception) {
            throw new RuntimeException(new TransactionNotFound("Claim not found"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean edit(String id , ClaimEditDto claimEditDto){

        try {
            boolean edited = false;
            if (claimEditDto.getBurialDate() != null) {
                TransactionDateEdit transactionDateEdit = new TransactionDateEdit();
                transactionDateEdit.setTransaction(id);
                transactionDateEdit.setType(DateType.BURIAL_DATE);
                transactionDateEdit.setValue(claimEditDto.getBurialDate());
                edited = transactionService.dateEdit(transactionDateEdit);

            }
            if (claimEditDto.getDeathDate() != null) {
                TransactionDateEdit edit = new TransactionDateEdit();
                edit.setTransaction(id);
                edit.setType(DateType.DEATH_DATE);
                edit.setValue(claimEditDto.getDeathDate());
                edited = transactionService.dateEdit(edit);

            }
            if (claimEditDto.getClaimantId() != null) {
                TransactionPartnerEdit edit = new TransactionPartnerEdit();
                edit.setTransaction(id);
                edit.setParnter(claimEditDto.getClaimantId());
                edit.setPartnerFunction(PartnerFunction.CLAIMANT);
                edited = transactionService.partnerEdit(edit);
            }
            if(claimEditDto.getPaidOutAmount() !=null){
                try {
                    TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                    transactionAmountInboundDto.setAmount(claimEditDto.getPaidOutAmount());
                    transactionAmountInboundDto.setTransaction(id);
                    transactionAmountInboundDto.setType(AmountType.PAID_OUT_AMOUNT);
                    transactionAmountService.save(transactionAmountInboundDto);
                    edited = true;
                } catch (Exception exception) {

                }
            }

            return edited;

        }catch (Exception e){
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
                VoucherCreateDto voucherCreateDto = new VoucherCreateDto();
                List<TransactionAmountOutboundDto> transactionAmountOutboundDtoList = transactionAmountService.getByTransaction(id);
                Iterator iterator = transactionAmountOutboundDtoList.stream()
                        .filter(a -> Objects.equals(a.getType().getCode(), AmountType.SERVICE_AMOUNT))
                        .toList().iterator();
                if (iterator.hasNext()) {
                    TransactionAmountOutboundDto transactionAmountOutboundDto = (TransactionAmountOutboundDto) iterator.next();
                    voucherCreateDto.setAmount(transactionAmountOutboundDto.getAmount());
                }
                List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id);
                Iterator partnerIterator = transactionPartnerDtoList.stream()
                        .filter(a -> Objects.equals(a.getFunction(), PartnerFunction.CUSTOMER))
                        .toList().iterator();
                if (partnerIterator.hasNext()) {
                    TransactionPartnerDto transactionPartnerDto = (TransactionPartnerDto) partnerIterator.next();
                    voucherCreateDto.setRecipientId(transactionPartnerDto.getPartner());
                }
                voucherCreateDto.setContractId(id);
                voucherService.create(voucherCreateDto);
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


    public ByteArrayResource generateClaimPdf(ClaimOutboundDto claimOutboundDto) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PDType1Font helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Centered text for "CASH CLAIM APPLICATION FORM"
                contentStream.setFont(helveticaBold, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(200, 750);
                contentStream.showText("CASH CLAIM APPLICATION FORM");
                contentStream.endText();

                // Section A title
                contentStream.setFont(helveticaBold, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(200, 710);
                contentStream.showText("SECTION A: CLAIM SUBMISSION DETAILS");
                contentStream.endText();

                // Section A details
                contentStream.setFont(helvetica, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 680);
                contentStream.showText("1. OFFICE OF CLAIM SUBMISSION: " + claimOutboundDto.getBranch().getCode());
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("2. POINT OF COLLECTION: " + claimOutboundDto.getBranch().getCode());
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("3. DATE OF CLAIM SUBMISSION: " + formatDate(claimOutboundDto.getCreationDate()));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("4. DATE OF CLAIM COLLECTION: " + formatDate(new Date()));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("5. CLAIM ADMINISTRATOR: " +
                        claimOutboundDto.getCustomer().getName1() + " " +
                        claimOutboundDto.getCustomer().getName2());
                contentStream.endText();

                // Section B title
                contentStream.setFont(helveticaBold, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(200, 570);
                contentStream.showText("SECTION B: INFORMATION OF POLICY HOLDER");
                contentStream.endText();

                // Section B details
                contentStream.setFont(helvetica, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 540);
                contentStream.showText("SURNAME: " + claimOutboundDto.getCustomer().getName2());
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("FULL NAMES: " +
                        claimOutboundDto.getCustomer().getName1() + " " +
                        claimOutboundDto.getCustomer().getName2() + " " +
                        claimOutboundDto.getCustomer().getName3());
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("ID NUMBER: " + claimOutboundDto.getCustomer().getIdentity().getNumber());
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("CONTACT NUMBER: " + claimOutboundDto.getCustomer().getNumber());
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("PHYSICAL ADDRESS: " + claimOutboundDto.getCustomer().getName4());
                contentStream.endText();

                // Section C: Deceased Information
                PartnerDto deceased = claimOutboundDto.getDeceased();
                if (deceased != null) {
                    contentStream.setFont(helveticaBold, 14);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(200, 430);
                    contentStream.showText("SECTION C: INFORMATION OF DECEASED");
                    contentStream.endText();

                    contentStream.setFont(helvetica, 12);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, 400);
                    contentStream.showText("SURNAME: " + deceased.getName2());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("FULL NAMES: " +
                            deceased.getName1() + " " +
                            deceased.getName2() + " " +
                            deceased.getName3());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("ID NUMBER: " + deceased.getIdentity().getNumber());
                    contentStream.endText();
                }

                // Section D: Information of Claimant (if not policy holder)
                PartnerDto claimant = claimOutboundDto.getClaimant();
                if (claimant != null) {
                    contentStream.setFont(helveticaBold, 14);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(200, 350);
                    contentStream.showText("SECTION D: INFORMATION OF CLAIMANT (IF NOT POLICY HOLDER)");
                    contentStream.endText();

                    contentStream.setFont(helvetica, 12);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, 320);
                    contentStream.showText("SURNAME: " + claimant.getName2());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("FULL NAMES: " +
                            claimant.getName1() + " " + claimant.getName2());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("ID NUMBER: " + claimant.getIdentity().getNumber());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("CONTACT NUMBER: " + claimant.getNumber());
                    contentStream.endText();
                }

                // Section E: Information of Cash Payout
                contentStream.setFont(helveticaBold, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(200, 250);
                contentStream.showText("SECTION E: INFORMATION OF CASH PAYOUT");
                contentStream.endText();

                contentStream.setFont(helvetica, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 220);
                contentStream.showText("CLAIM PAID OUT TO POLICY HOLDER? " +
                        (claimOutboundDto.getClaimant() != null ? "Yes" : "No"));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("NOMINATED BENEFICIARY: " +
                        (claimOutboundDto.getClaimant() != null ?
                                claimOutboundDto.getClaimant().getName1() + " " +
                                        claimOutboundDto.getClaimant().getName2() : "N/A"));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("POINT OF COLLECTION: " + claimOutboundDto.getBranch().getCode());
                contentStream.endText();

                BankAccountDto bankDetails = claimOutboundDto.getBankDetails();
                if (bankDetails != null) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, 190);
                    contentStream.showText("NAME OF BANK: " + bankDetails.getBankName());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("ACCOUNT HOLDER FULL NAMES: " + bankDetails.getAccountHolder());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("ACCOUNT NUMBER: " + bankDetails.getAccountNumber());
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("ACCOUNT TYPE: " + bankDetails.getAccountType());
                    contentStream.endText();
                }
                contentStream.close();

                document.save(byteArrayOutputStream);
            }
            return new ByteArrayResource(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating the claim PDF: " + e.getMessage());
        }
    }



    private String formatDate(Date date) {
        if (date == null) {
            return "N/A";
        }
        return date.toString();
    }
}
