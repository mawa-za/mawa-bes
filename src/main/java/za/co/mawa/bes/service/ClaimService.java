package za.co.mawa.bes.service;

import jakarta.mail.Part;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.claim.*;
import za.co.mawa.bes.dto.comment.CommentDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
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
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;

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
    @Autowired
    BankAccountService bankAccountService;
    @Autowired
    UserService userService;
    @Autowired
    TransactionLinkRepository transactionLinkRepository;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;


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
                try {
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
            } catch (Exception e) {
            }

            if (transactionDto.getDescription() == null) {
                claimOutboundDto.setDescription(transactionDto.getSubDescription());
            } else {
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
            try {
                List<TransactionAmountEntity> transactionAmountEntities = transactionAmountRepository.getByTransaction(id);
                for (TransactionAmountEntity transactionAmount : transactionAmountEntities) {
                    if (transactionAmount.getType().equals(AmountType.PAID_OUT_AMOUNT)) {
                        TransactionAmountOutboundDto transactionAmountOutboundDto = new TransactionAmountOutboundDto();
                        transactionAmountOutboundDto.setId(transactionAmount.getId());
                        transactionAmountOutboundDto.setTransaction(id);
                        transactionAmountOutboundDto.setAmount(transactionAmount.getAmount());
                        transactionAmountOutboundDto.setChangedBy(transactionAmount.getChangedBy());
                        transactionAmountOutboundDto.setCreatedBy(transactionAmount.getCreatedBy());
                        claimOutboundDto.setPaidOutAmount(transactionAmountOutboundDto);
                    }
                }
            } catch (Exception e) {
            }

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

            } catch (Exception e) {
            }

            return claimOutboundDto;
        } catch (TransactionNotFound exception) {
            throw new RuntimeException(new TransactionNotFound("Claim not found"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean edit(String id, ClaimEditDto claimEditDto) {

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
            if (claimEditDto.getPaidOutAmount() != null) {
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
            } else {
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // margins and spacing
                float marginX = 50;
                float marginY = 750;
                float lineHeight = 14;
                float tableRowHeight = 19;
                float pageWidth = page.getMediaBox().getWidth();
                float tableWidth = pageWidth - 100;

                // title
                contentStream.setFont(fontBold, 18);
                String title = "CASH CLAIM APPLICATION FORM";
                float titleWidth = fontBold.getStringWidth(title) / 1000 * 18;
                float titleX = (pageWidth - titleWidth) / 2;

                contentStream.beginText();
                contentStream.newLineAtOffset(titleX, marginY);
                contentStream.showText(title);
                contentStream.endText();
                marginY -= lineHeight * 2;

                BiConsumer<String[], Float> drawTableRow = (values, yPos) -> {
                    try {
                        float cellWidth = tableWidth / values.length;
                        float xPos = marginX;

                        contentStream.setFont(fontRegular, 10);

                        // Drawing top horizontal border
                        contentStream.moveTo(marginX, yPos);
                        contentStream.lineTo(marginX + tableWidth, yPos);
                        contentStream.stroke();

                        // Drawing all vertical borders first (no duplicates)
                        for (int i = 0; i <= values.length; i++) {
                            float currentXPos = marginX + (i * cellWidth);
                            contentStream.moveTo(currentXPos, yPos);
                            contentStream.lineTo(currentXPos, yPos - tableRowHeight);
                            contentStream.stroke();
                        }

                        // Adding text in each cell
                        xPos = marginX;
                        for (int i = 0; i < values.length; i++) {
                            // Add text to cell
                            contentStream.beginText();
                            contentStream.newLineAtOffset(xPos + 5, yPos - 12);
                            contentStream.showText(values[i] == null ? "" : values[i]);
                            contentStream.endText();

                            xPos += cellWidth;
                        }

                        // Drawing bottom horizontal border
                        contentStream.moveTo(marginX, yPos - tableRowHeight);
                        contentStream.lineTo(marginX + tableWidth, yPos - tableRowHeight);
                        contentStream.stroke();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

                BiConsumer<String, Float> addCenteredSectionTitle = (text, yPos) -> {
                    try {
                        float textWidth = fontBold.getStringWidth(text) / 1000 * 12;
                        float centerX = (pageWidth - textWidth) / 2;
                        contentStream.setFont(fontBold, 12);
                        contentStream.beginText();
                        contentStream.newLineAtOffset(centerX, yPos);
                        contentStream.showText(text);
                        contentStream.endText();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

                // Section A: Claim Submission Details (Table)
                addCenteredSectionTitle.accept("SECTION A: CLAIM SUBMISSION DETAILS", marginY);
                marginY -= lineHeight;
                drawTableRow.accept(new String[]{"CLAIM NUMBER", claimOutboundDto.getNumber() != null ? claimOutboundDto.getNumber() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"OFFICE OF CLAIM SUBMISSION", claimOutboundDto.getBranch() != null ? claimOutboundDto.getBranch().getCode() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"POINT OF COLLECTION", claimOutboundDto.getBranch() != null ? claimOutboundDto.getBranch().getCode() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"DATE OF CLAIM SUBMISSION", claimOutboundDto.getCreationDate() != null ? claimOutboundDto.getCreationDate().toString() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"DATE OF CLAIM COLLECTION", ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"CLAIM ADMINISTRATOR", claimOutboundDto.getCustomer() != null ? claimOutboundDto.getCustomer().getName1() : ""}, marginY);
                marginY -= tableRowHeight * 2;

                String policyNumber = null;
                try{
                    List<TransactionLinkEntity> transactionLinkEntity = transactionLinkRepository.getTransactionLink(claimOutboundDto.getId(), TransactionType.CLAIM);

                    for(TransactionLinkEntity entity: transactionLinkEntity){
                        String membershipID = entity.getTransactionLinkPKEntity().getTransaction1();
                        MembershipDto membershipDto = membershipService.get(membershipID);
                        policyNumber = membershipDto.getNumber();
                    }
                }catch(Exception e){

                }

                // Section B: Policy Holder Information
                addCenteredSectionTitle.accept("SECTION B: POLICY HOLDER INFORMATION", marginY);
                marginY -= lineHeight;
                drawTableRow.accept(new String[]{"POLICY NUMBER", policyNumber != null ? policyNumber : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"SURNAME", claimOutboundDto.getCustomer() != null ? claimOutboundDto.getCustomer().getName1() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"FULL NAMES", claimOutboundDto.getCustomer() != null ? claimOutboundDto.getCustomer().getName1() + " " + claimOutboundDto.getCustomer().getName2() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"CONTACT NUMBER", ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"ID NUMBER", claimOutboundDto.getCustomer() != null && claimOutboundDto.getCustomer().getIdentity() != null ? claimOutboundDto.getCustomer().getIdentity().getNumber() : ""}, marginY);
                marginY -= tableRowHeight * 2;

                // Section C: Deceased Information
                PartnerDto deceased = claimOutboundDto.getDeceased();
                if (deceased != null) {
                    addCenteredSectionTitle.accept("SECTION C: DECEASED INFORMATION", marginY);
                    marginY -= lineHeight;
                    drawTableRow.accept(new String[]{"SURNAME", deceased.getName2() != null ? deceased.getName2() : ""}, marginY);
                    marginY -= tableRowHeight;
                    drawTableRow.accept(new String[]{"FULL NAMES", deceased.getName1() != null ? deceased.getName1() + " " + deceased.getName2() + " " + deceased.getName3() : ""}, marginY);
                    marginY -= tableRowHeight;
                    drawTableRow.accept(new String[]{"ID NUMBER", deceased.getIdentity() != null ? deceased.getIdentity().getNumber() : ""}, marginY);
                    marginY -= tableRowHeight * 2;
                }

                // Section D: Claimant Information
                PartnerDto claimant = claimOutboundDto.getClaimant();
                if (claimant != null) {
                    addCenteredSectionTitle.accept("SECTION D: CLAIMANT INFORMATION (IF NOT POLICY HOLDER)", marginY);
                    marginY -= lineHeight;
                    drawTableRow.accept(new String[]{"SURNAME", claimant.getName2() != null ? claimant.getName2() : ""}, marginY);
                    marginY -= tableRowHeight;
                    drawTableRow.accept(new String[]{"FULL NAMES", claimant.getName1() != null ? claimant.getName1() + " " + claimant.getName2() : ""}, marginY);
                    marginY -= tableRowHeight;
                    drawTableRow.accept(new String[]{"ID NUMBER", claimant.getIdentity() != null ? claimant.getIdentity().getNumber() : ""}, marginY);
                    marginY -= tableRowHeight;
                    drawTableRow.accept(new String[]{"CONTACT NUMBER", ""}, marginY);
                    marginY -= tableRowHeight * 2;
                }

                // Section E: Bank Details + Signature Field
                addCenteredSectionTitle.accept("SECTION E: CASH PAYOUT INFORMATION", marginY);
                marginY -= lineHeight;

                BankAccountCreateDto bankAccount = null;

                try{
                    BankAccountDto bankAccountDto = bankAccountService.getList(claimOutboundDto.getId()).iterator().next();
                    bankAccount = new BankAccountCreateDto();
                    bankAccount.setAccountHolder(bankAccountDto.getAccountHolder());
                    bankAccount.setAccountType(bankAccountDto.getAccountType().getCode());
                    bankAccount.setBankName(bankAccountDto.getBankName().getCode());
                    bankAccount.setAccountNumber(bankAccountDto.getAccountNumber());
                    bankAccount.setBranchCode(bankAccountDto.getBranchCode());
                    bankAccount.setObjectId(claimOutboundDto.getId());
                }catch(Exception e){

                }

                String accountHolderId = null;
                String fullName = null;
                try{
                    List <PartnerEntity> accountHolder = partnerRepository.findByFullName(bankAccount.getAccountHolder());
                    if(accountHolder != null){
                        PartnerEntity partner = accountHolder.getFirst();
                        List<PartnerIdentityEntity> identityEntities = partnerIdentityRepository.findByPartner(partner.getId());
                        PartnerIdentityEntity partnerIdentity = identityEntities.getFirst();
                        accountHolderId = partnerIdentity.getPartnerIdentityPK().getValue();
                        fullName = bankAccount.getAccountHolder();
                    }

                }catch (Exception e){

                }

                drawTableRow.accept(new String[]{
                        "CLAIM PAYOUT AMOUNT",
                        claimOutboundDto.getPaidOutAmount() != null && claimOutboundDto.getPaidOutAmount().getAmount() != null
                                ? String.valueOf(claimOutboundDto.getPaidOutAmount().getAmount())
                                : ""
                }, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"CLAIM PAID OUT TO POLICY HOLDER", ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"NOMINATED BENEFICIARY", ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"POINT OF COLLECTION", claimOutboundDto.getBranch() != null ? claimOutboundDto.getBranch().getCode() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"BANK NAME", bankAccount != null && bankAccount.getBankName() != null? bankAccount.getBankName() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"ACCOUNT HOLDER FULL NAMES", fullName != null ? fullName : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"ACCOUNT HOLDER ID NUMBER", accountHolderId != null ? accountHolderId : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"ACCOUNT NUMBER", bankAccount != null && bankAccount.getAccountNumber() != null ? bankAccount.getAccountNumber() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"ACCOUNT TYPE", bankAccount != null && bankAccount.getAccountType() != null? bankAccount.getAccountType() : ""}, marginY);
                marginY -= tableRowHeight;
                drawTableRow.accept(new String[]{"ACCOUNT HOLDER CONTACT NUMBER", ""}, marginY);
                marginY -= tableRowHeight;

                marginY -= 20;
                float dateX = marginX + 250;
                float signatureX = marginX + 50;

                contentStream.setFont(fontRegular, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(marginX , marginY);
                contentStream.showText("Claimant Signature:");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(dateX , marginY);
                contentStream.showText("Date:");
                contentStream.endText();

                contentStream.moveTo(marginX + 91, marginY - 10);
                contentStream.lineTo(signatureX + 185, marginY - 10);
                contentStream.stroke();

                contentStream.moveTo(dateX + 28, marginY - 10);
                contentStream.lineTo(dateX + 200, marginY - 10);
                contentStream.stroke();

                contentStream.close();
                document.save(outputStream);
                return new ByteArrayResource(outputStream.toByteArray());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}

