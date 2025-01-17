package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.BankFileXmlDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionEditDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkDto;
import za.co.mawa.bes.exception.RoleDoesNotExist;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class BankFileService {

    @Autowired
    TransactionService transactionService;
    @Autowired
    AttachmentService attachmentService;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    SettingService settingService;
    @Autowired
    BankAccountService bankAccountService;
    @Autowired
    PaymentRequestService paymentRequestService;

    @Autowired
    PartnerService partnerService;
    @Autowired
    PartnerIdentityService partnerIdentityService;

    public TransactionDto createPaymentBatch() {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.PAYMENT_BATCH);
        return transactionService.create(transactionCreateDto);
    }


    public za.co.mawa.bes.dto.File generateBankFile(List<String> paymentRequestIds) throws Exception {
        try {
            List<PaymentRequestDto> paymentRequestDtoList = new ArrayList<>();
            TransactionDto transactionDto = createPaymentBatch();
            BankFileXmlDto bankFileXmlDto = new BankFileXmlDto();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element documentElement = doc.createElement("Document");
            documentElement.setAttribute("xmlns", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03");
            doc.appendChild(documentElement);
            Element cstmrCdtTrfInitn = doc.createElement("CstmrCdtTrfInitn");

            BigDecimal totalAmount = new BigDecimal(0);
            for (String id : paymentRequestIds) {
                PaymentRequestDto paymentRequestDto = paymentRequestService.get(id);
                if (!paymentRequestDto.getAmount().equals(null)) {
                    totalAmount = totalAmount.add(paymentRequestDto.getAmount());
                }
                paymentRequestDtoList.add(paymentRequestDto);
            }
            bankFileXmlDto.setId(transactionDto.getNumber());
            bankFileXmlDto.setAmount(totalAmount);
            bankFileXmlDto.setNumberOfTransactions(paymentRequestDtoList.size());
            Element grpHdr = groupHeader(bankFileXmlDto, doc);
            cstmrCdtTrfInitn.appendChild(grpHdr);

            for (PaymentRequestDto paymentRequestDto : paymentRequestDtoList) {
                cstmrCdtTrfInitn.appendChild(PaymentInformation(paymentRequestDto, doc));
            }

            documentElement.appendChild(cstmrCdtTrfInitn);
            // Transform the DOM Document to an XML File
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            DOMSource domSource = new DOMSource(doc);
//            StreamResult streamResult = new StreamResult(new File(System.getProperty("user.dir") + "/iso_payment_" + Conversion.dateToString(new Date()) + ".xml"));
            StreamResult streamResult = new StreamResult(new ByteArrayOutputStream());
            transformer.transform(domSource, streamResult);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(outputStream));

            // Convert byte array output to Base64
            byte[] xmlBytes = outputStream.toByteArray();
            for (PaymentRequestDto paymentRequestDto : paymentRequestDtoList) {
                TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                transactionLinkDto.setTransaction1(transactionDto.getId());
                transactionLinkDto.setTransaction2(paymentRequestDto.getId());
                transactionLinkDto.setType(TransactionType.PAYMENT_REQUEST);
                transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                transactionService.addLink(transactionLinkDto);

//                TransactionEditDto transactionEditDto = new TransactionEditDto();
//                transactionEditDto.setId(paymentRequestDto.getId());
//                transactionEditDto.setStatus(Status.PROCESSED);
//                transactionService.edit(transactionEditDto);
            }
            za.co.mawa.bes.dto.File file = new za.co.mawa.bes.dto.File();
            file.setName(getInitParty() +" - "+transactionDto.getNumber());
            file.setType("xml");
            file.setContent(Base64.getEncoder().encodeToString(xmlBytes));
            file.setOwner(getInitParty());
            return file;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Element groupHeader(BankFileXmlDto bankFileXmlDto, Document doc) {
        Element grpHdr = doc.createElement("GrpHdr");
        try {

            Element MsgId = doc.createElement("MsgId");
            MsgId.appendChild(doc.createTextNode(bankFileXmlDto.getId()));
            grpHdr.appendChild(MsgId);

            Element creDtTm = doc.createElement("CreDtTm");

            creDtTm.appendChild(doc.createTextNode(Conversion.dateTimeToString3(new Date())));
            grpHdr.appendChild(creDtTm);

            Element nbOfTxs = doc.createElement("NbOfTxs");
            nbOfTxs.appendChild(doc.createTextNode(Integer.toString(bankFileXmlDto.getNumberOfTransactions())));
            grpHdr.appendChild(nbOfTxs);

            Element ctrlSum = doc.createElement("CtrlSum");
            ctrlSum.appendChild(doc.createTextNode(bankFileXmlDto.getAmount().toString()));
            grpHdr.appendChild(ctrlSum);

            Element initgPty = doc.createElement("InitgPty");
            Element initgPtyName = doc.createElement("Nm");
            initgPtyName.appendChild(doc.createTextNode(getInitParty()));
            initgPty.appendChild(initgPtyName);
            grpHdr.appendChild(initgPty);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return grpHdr;
    }

    private Element PaymentInformation(PaymentRequestDto paymentRequestDto, Document doc) {
        Element pmtInf = doc.createElement("PmtInf");
        try {

            Element pmtInfId = doc.createElement("PmtInfId");
            pmtInfId.appendChild(doc.createTextNode(paymentRequestDto.getNumber()));
            pmtInf.appendChild(pmtInfId);

            Element pmtMtd = doc.createElement("PmtMtd");
            pmtMtd.appendChild(doc.createTextNode("TRF"));
            pmtInf.appendChild(pmtMtd);

            Element btchBookg = doc.createElement("BtchBookg");
            btchBookg.appendChild(doc.createTextNode("false"));
            pmtInf.appendChild(btchBookg);

            Element pmtNbOfTxs = doc.createElement("NbOfTxs");
            pmtNbOfTxs.appendChild(doc.createTextNode("1"));
            pmtInf.appendChild(pmtNbOfTxs);

            Element pmtCtrlSum = doc.createElement("CtrlSum");
            pmtCtrlSum.appendChild(doc.createTextNode(paymentRequestDto.getAmount().toString()));
            pmtInf.appendChild(pmtCtrlSum);

            Element pmtTpInf = doc.createElement("PmtTpInf");
            Element svcLvl = doc.createElement("SvcLvl");
            Element svcLvlCd = doc.createElement("Cd");
            svcLvlCd.appendChild(doc.createTextNode("SDVA"));
            svcLvl.appendChild(svcLvlCd);
            pmtTpInf.appendChild(svcLvl);
            pmtInf.appendChild(pmtTpInf);

            Element reqdExctnDt = doc.createElement("ReqdExctnDt");
            reqdExctnDt.appendChild(doc.createTextNode(Conversion.dateToString(new Date())));
            pmtInf.appendChild(reqdExctnDt);

            // Debtor Information
            Element dbtr = doc.createElement("Dbtr");
            Element dbtrName = doc.createElement("Nm");
            dbtrName.appendChild(doc.createTextNode(getBankAccountHolder()));
            dbtr.appendChild(dbtrName);
            pmtInf.appendChild(dbtr);

            // Debtor Account
            Element dbtrAcct = doc.createElement("DbtrAcct");
            Element dbtrAcctId = doc.createElement("Id");
            Element dbtrAcctOthr = doc.createElement("Othr");
            Element dbtrAcctOthrId = doc.createElement("Id");
            dbtrAcctOthrId.appendChild(doc.createTextNode(getBankAccountNumber()));
            dbtrAcctOthr.appendChild(dbtrAcctOthrId);
            dbtrAcctId.appendChild(dbtrAcctOthr);
            dbtrAcct.appendChild(dbtrAcctId);

            Element tP = doc.createElement("Tp");
            Element cD = doc.createElement("Cd");
            cD.appendChild(doc.createTextNode("CACC"));
            tP.appendChild(cD);
            dbtrAcct.appendChild(tP);
            pmtInf.appendChild(dbtrAcct);

            Element dbtrAgt = doc.createElement("DbtrAgt");
            Element finInstnId = doc.createElement("FinInstnId");
            Element clrSysMmbId = doc.createElement("ClrSysMmbId");
            Element mmbId = doc.createElement("MmbId");
            Element brnchId = doc.createElement("BrnchId");
            Element id = doc.createElement("Id");
            brnchId.appendChild(id);
            id.appendChild(doc.createTextNode(getBankAccountBranch()));
            mmbId.appendChild(doc.createTextNode(getBankAccountBranch()));
            dbtrAgt.appendChild(finInstnId);
            dbtrAgt.appendChild(brnchId);
            finInstnId.appendChild(clrSysMmbId);
            clrSysMmbId.appendChild(mmbId);
            pmtInf.appendChild(dbtrAgt);

            Element cdtTrfTxInf = creditTransfer(paymentRequestDto, doc);

            pmtInf.appendChild(cdtTrfTxInf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return pmtInf;
    }

    private Element creditTransfer(PaymentRequestDto paymentRequestDto, Document doc) {
        BankAccountDto bankAccountDto;
        if (paymentRequestDto.getPaymentMethod().getCode().equals("EFT")) {
            bankAccountDto = bankAccountService.getList(paymentRequestDto.getId()).iterator().next();
            bankAccountDto.setBranchCode(bankAccountService.getUBC(bankAccountDto.getBankName().getCode()));
        } else {
            bankAccountDto = new BankAccountDto();
            bankAccountDto.setAccountHolder(getPettyCashBankAccountHolder());
            bankAccountDto.setBranchCode(getPettyCashBankAccountBranch());
            bankAccountDto.setAccountNumber(getPettyCashBankAccountNumber());
            FieldOptionDto fieldOptionDto = new FieldOptionDto();
            fieldOptionDto.setCode(getPettyCashBankAccountType());
            bankAccountDto.setAccountType(fieldOptionDto);
            bankAccountDto.setBranchCode(getPettyCashBankAccountBranch());
        }

        Element cdtTrfTxInf = doc.createElement("CdtTrfTxInf");
        try {
            Element pmtId = doc.createElement("PmtId");
            Element endToEndId = doc.createElement("EndToEndId");
            String paymentReason;
            try {
                paymentReason = paymentRequestDto.getPaymentReason().getDescription();
            } catch (Exception ex) {
                paymentReason = bankAccountDto.getAccountHolder();
            }
            endToEndId.appendChild(doc.createTextNode(paymentReason));
            pmtId.appendChild(endToEndId);
            cdtTrfTxInf.appendChild(pmtId);
            Element amt = doc.createElement("Amt");
            Element instdAmt = doc.createElement("InstdAmt");
            instdAmt.setAttribute("Ccy", "ZAR");
            instdAmt.appendChild(doc.createTextNode(paymentRequestDto.getAmount().toString()));
            amt.appendChild(instdAmt);
            cdtTrfTxInf.appendChild(amt);

            Element cdtrAgt = doc.createElement("CdtrAgt");
            Element finInstnId = doc.createElement("FinInstnId");
            Element clrSysMmbId = doc.createElement("ClrSysMmbId");
            Element mmbId = doc.createElement("MmbId");
            mmbId.appendChild(doc.createTextNode(bankAccountDto.getBranchCode()));
            clrSysMmbId.appendChild(mmbId);
            finInstnId.appendChild(clrSysMmbId);
            cdtrAgt.appendChild(finInstnId);
            Element brnchId = doc.createElement("BrnchId");
            Element id = doc.createElement("Id");
            id.appendChild(doc.createTextNode(bankAccountDto.getBranchCode()));
            brnchId.appendChild(id);
            cdtrAgt.appendChild(brnchId);

            Element cdtr = doc.createElement("Cdtr");
            Element nm = doc.createElement("Nm");
            nm.appendChild(doc.createTextNode(bankAccountDto.getAccountHolder()));
            cdtr.appendChild(nm);

            Element cdtrAcct = doc.createElement("CdtrAcct");
            Element cdtrAcctId = doc.createElement("Id");
            Element cdtrAcctIdothr = doc.createElement("Othr");
            Element cdtrAcctIdothrId = doc.createElement("Id");
            cdtrAcctIdothrId.appendChild(doc.createTextNode(bankAccountDto.getAccountNumber()));
            cdtrAcctIdothr.appendChild(cdtrAcctIdothrId);
            cdtrAcctId.appendChild(cdtrAcctIdothr);

            Element tp = doc.createElement("Tp");
            Element cd = doc.createElement("Cd");
            String accountType;
            try {
                accountType = bankAccountDto.getAccountType().getCode();
            } catch (Exception ex) {
                accountType = "CHEQUE";
            }
            if (accountType.equals("CHEQUE")) {
                cd.appendChild(doc.createTextNode("CACC"));
            } else if (accountType.equals("SAVINGS")) {
                cd.appendChild(doc.createTextNode("SVGS"));
            }
            tp.appendChild(cd);
            cdtrAcct.appendChild(cdtrAcctId);
            cdtrAcct.appendChild(tp);

            Element rmtInf = doc.createElement("RmtInf");
            Element ustrd = doc.createElement("Ustrd");
            String reference;
            List<PartnerIdentityDto> identityDtoArrayList =
                    partnerIdentityService.getAll(paymentRequestDto.getRecipient().getId()).stream()
                            .filter(a -> Objects.equals(a.getType().getCode(), "ACCOUNT-NUMBER"))
                            .toList();
            if (!identityDtoArrayList.isEmpty()) {
                reference = identityDtoArrayList.iterator().next().getNumber();
            } else {
                reference = paymentRequestDto.getReference();
            }

            ustrd.appendChild(doc.createTextNode(reference));
            rmtInf.appendChild(ustrd);

            cdtTrfTxInf.appendChild(cdtrAgt);
            cdtTrfTxInf.appendChild(cdtr);
            cdtTrfTxInf.appendChild(cdtrAcct);
            cdtTrfTxInf.appendChild(rmtInf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cdtTrfTxInf;
    }

    private String getInitParty() {
        Properties properties = settingService.getSettings("TENANT");
        try {
            return properties.get("COMPANY-NAME").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getBankAccountHolder() {
        Properties properties = settingService.getSettings("EFT-BANK-ACCOUNT");
        try {
            return properties.get("ACCOUNT-HOLDER").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getBankAccountNumber() {
        Properties properties = settingService.getSettings("EFT-BANK-ACCOUNT");
        try {
            return properties.get("ACCOUNT-NUMBER").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getBankAccountBranch() {
        Properties properties = settingService.getSettings("EFT-BANK-ACCOUNT");
        try {
            return properties.get("BRANCH-CODE").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getPettyCashBankAccountHolder() {
        Properties properties = settingService.getSettings("CASH-BANK-ACCOUNT");
        try {
            return properties.get("ACCOUNT-HOLDER").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getPettyCashBankAccountNumber() {
        Properties properties = settingService.getSettings("CASH-BANK-ACCOUNT");
        try {
            return properties.get("ACCOUNT-NUMBER").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getPettyCashBankAccountType() {
        Properties properties = settingService.getSettings("CASH-BANK-ACCOUNT");
        try {
            return properties.get("ACCOUNT-TYPE").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getPettyCashBankAccountBranch() {
        Properties properties = settingService.getSettings("CASH-BANK-ACCOUNT");
        try {
            return properties.get("BRANCH-CODE").toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
