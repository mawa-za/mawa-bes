package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.BankFileXmlDao;
import za.co.mawa.bes.dto.BankFileXmlCreateDto;
import za.co.mawa.bes.dto.BankFileXmlDto;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.math.BigDecimal;
import java.util.Base64;

@Service
public class BankFileXmlService implements BankFileXmlDao {

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    PaymentRequestService paymentRequestService;
    @Override
    public String createBankFile(BankFileXmlCreateDto bankFileCreateXmlDto) throws Exception {


    try{

;

        BankFileXmlDto bankFileXmlDto =   AssignBankFileXml(bankFileCreateXmlDto);
//        BankFileXmlDto bankFileXmlDto = new BankFileXmlDto();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();

        Element documentElement = doc.createElement("Document");
        documentElement.setAttribute("xmlns", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03");
        doc.appendChild(documentElement);

        Element cstmrCdtTrfInitn = doc.createElement("CstmrCdtTrfInitn");

//        Group Hearder
        Element  grpHdr = groupHeader(bankFileXmlDto, doc);
        cstmrCdtTrfInitn.appendChild(grpHdr);


        // Payment Information <PmtInf>
        Element   pmtInf = PaymentInformation(bankFileXmlDto, doc);
        cstmrCdtTrfInitn.appendChild(pmtInf);

        documentElement.appendChild(cstmrCdtTrfInitn);


        // Transform the DOM Document to an XML File
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(new File(  System.getProperty("user.dir") + "/iso_payment.xml"));
        transformer.transform(domSource, streamResult);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(outputStream));

        // Convert byte array output to Base64
        byte[] xmlBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(xmlBytes);



       }catch(Exception e){
        throw new RuntimeException(e);
       }

    }

    @Override
    public BankFileXmlDto AssignBankFileXml(BankFileXmlCreateDto bankFileCreateXmlDto) throws Exception {

        PaymentRequestDto paymentRequestDto = paymentRequestService.get(bankFileCreateXmlDto.getPaymentRequestId());
        BankFileXmlDto bankFileXmlDto = new BankFileXmlDto();
        bankFileXmlDto.setIdentification(paymentRequestDto.getBankAccount().getId());

        bankFileXmlDto.setAmount(paymentRequestDto.getAmount().doubleValue());
        bankFileXmlDto.setPaymentMethod(paymentRequestDto.getPaymentMethod().getType());
        bankFileXmlDto.setCreationDateTime(paymentRequestDto.getCreatedDate().toString());
        bankFileXmlDto.setName("");
        bankFileXmlDto.setCode(paymentRequestDto.getBranch().getCode());
        bankFileXmlDto.setPaymentInformationIdentification("");
        bankFileXmlDto.setBatchBooking(false);
        bankFileXmlDto.setNumberOfTransactions(1);
        bankFileXmlDto.setControlSum(0.0);
        bankFileXmlDto.setRequestedExecutionDate("");
        bankFileXmlDto.setMemberId("");
        bankFileXmlDto.setEndToEndIdentification("");
        bankFileXmlDto.setInstructedAmount(0.0);
        bankFileXmlDto.setProprietary("");
        bankFileXmlDto.setUnStructured(paymentRequestDto.getReference());

        return bankFileXmlDto;
    }

    private Element groupHeader( BankFileXmlDto bankFileXmlDto, Document doc)
    {
        Element  grpHdr = doc.createElement("GrpHdr");
       try {

        Element MsgId = doc.createElement("MsgId");
        MsgId.appendChild(doc.createTextNode(bankFileXmlDto.getMessageIdentification()));
        grpHdr.appendChild(MsgId);

        Element creDtTm = doc.createElement("CreDtTm");
        creDtTm.appendChild(doc.createTextNode(bankFileXmlDto.getCreationDateTime()));
        grpHdr.appendChild(creDtTm);

        Element nbOfTxs = doc.createElement("NbOfTxs");
        nbOfTxs.appendChild(doc.createTextNode(Integer.toString( bankFileXmlDto.getNumberOfTransactions())));
        grpHdr.appendChild(nbOfTxs);


        Element ctrlSum = doc.createElement("CtrlSum");
        ctrlSum.appendChild(doc.createTextNode( Double.toString(bankFileXmlDto.getControlSum()) ));
        grpHdr.appendChild(ctrlSum);

        Element initgPty = doc.createElement("InitgPty");
        Element initgPtyName = doc.createElement("Nm");
        initgPtyName.appendChild(doc.createTextNode(bankFileXmlDto.getName()));
        initgPty.appendChild(initgPtyName);
        grpHdr.appendChild(initgPty);
       }catch(Exception e){
           throw new RuntimeException(e);
       }
        return  grpHdr;
    }

    private Element PaymentInformation (BankFileXmlDto bankFileXmlDto, Document doc)
    {

        Element pmtInf = doc.createElement("PmtInf");
        try {


        Element pmtInfId = doc.createElement("PmtInfId");
        pmtInfId.appendChild(doc.createTextNode(bankFileXmlDto.getPaymentInformationIdentification()));
        pmtInf.appendChild(pmtInfId);

        Element pmtMtd = doc.createElement("PmtMtd");
        pmtMtd.appendChild(doc.createTextNode(bankFileXmlDto.getPaymentMethod()));
        pmtInf.appendChild(pmtMtd);

        Element btchBookg = doc.createElement("BtchBookg");
        btchBookg.appendChild(doc.createTextNode(Boolean.toString(bankFileXmlDto.getBatchBooking())));
        pmtInf.appendChild(btchBookg);

        Element pmtNbOfTxs = doc.createElement("NbOfTxs");
        pmtNbOfTxs.appendChild(doc.createTextNode(Integer.toString(bankFileXmlDto.getNumberOfTransactions())));
        pmtInf.appendChild(pmtNbOfTxs);

        Element pmtCtrlSum = doc.createElement("CtrlSum");
        pmtCtrlSum.appendChild(doc.createTextNode(Double.toString(bankFileXmlDto.getControlSum())));
        pmtInf.appendChild(pmtCtrlSum);

        Element pmtTpInf = doc.createElement("PmtTpInf");
        Element svcLvl = doc.createElement("SvcLvl");
        Element svcLvlCd = doc.createElement("Cd");
        svcLvlCd.appendChild(doc.createTextNode(bankFileXmlDto.getCode()));
        svcLvl.appendChild(svcLvlCd);
        pmtTpInf.appendChild(svcLvl);
        pmtInf.appendChild(pmtTpInf);

        Element reqdExctnDt = doc.createElement("ReqdExctnDt");
        reqdExctnDt.appendChild(doc.createTextNode(bankFileXmlDto.getRequestedExecutionDate()));
        pmtInf.appendChild(reqdExctnDt);

        // Debtor Information
        Element dbtr = doc.createElement("Dbtr");
        Element dbtrName = doc.createElement("Nm");
        dbtrName.appendChild(doc.createTextNode(bankFileXmlDto.getName()));
        dbtr.appendChild(dbtrName);
        pmtInf.appendChild(dbtr);

        // Debtor Account
        Element dbtrAcct = doc.createElement("DbtrAcct");
        Element dbtrAcctId = doc.createElement("Id");
        Element dbtrAcctOthr = doc.createElement("Othr");
        Element dbtrAcctOthrId = doc.createElement("Id");
        dbtrAcctOthrId.appendChild(doc.createTextNode(bankFileXmlDto.getIdentification()));
        dbtrAcctOthr.appendChild(dbtrAcctOthrId);
        dbtrAcctId.appendChild(dbtrAcctOthr);
        dbtrAcct.appendChild(dbtrAcctId);

        Element tP = doc.createElement("Tp");
        Element cD = doc.createElement("Cd");
        cD.appendChild(doc.createTextNode(bankFileXmlDto.getCode()));
        tP.appendChild(cD);
        dbtrAcct.appendChild(tP);
        pmtInf.appendChild(dbtrAcct);

        Element   dbtrAgt  =  doc.createElement("DbtrAgt");
        Element   finInstnId  =  doc.createElement("FinInstnId");
        Element clrSysMmbId = doc.createElement("ClrSysMmbId");
        Element mmbId = doc.createElement("MmbId");
        Element brnchId = doc.createElement("BrnchId");
        Element id = doc.createElement("Id");
        brnchId.appendChild(id);
        id.appendChild(doc.createTextNode(bankFileXmlDto.getIdentification()));
        mmbId.appendChild(doc.createTextNode(bankFileXmlDto.getMemberId()));
        dbtrAgt.appendChild(finInstnId);
        dbtrAgt.appendChild(brnchId);
        finInstnId.appendChild(clrSysMmbId);
        clrSysMmbId.appendChild(mmbId);
        pmtInf.appendChild(dbtrAgt);


//      Credit Transfer
        Element cdtTrfTxInf = creditTransfer(bankFileXmlDto,doc);


        pmtInf.appendChild(cdtTrfTxInf);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        return pmtInf;
    }

    private Element creditTransfer (BankFileXmlDto bankFileXmlDto, Document doc)
    {
        Element cdtTrfTxInf = doc.createElement("CdtTrfTxInf");
        try {
        Element pmtId = doc.createElement("PmtId");
        Element endToEndId = doc.createElement("EndToEndId");
        endToEndId.appendChild(doc.createTextNode(bankFileXmlDto.getEndToEndIdentification()));
        pmtId.appendChild(endToEndId);
        cdtTrfTxInf.appendChild(pmtId);


        Element amt = doc.createElement("Amt");
        Element instdAmt = doc.createElement("InstdAmt");
        instdAmt.appendChild(doc.createTextNode(Double.toString(bankFileXmlDto.getInstructedAmount()) ));
        amt.appendChild(instdAmt);
        cdtTrfTxInf.appendChild(amt);

        Element cdtrAgt = doc.createElement("CdtrAgt");
        Element   finInstnId  =  doc.createElement("FinInstnId");
        Element clrSysMmbId = doc.createElement("ClrSysMmbId");
        Element mmbId = doc.createElement("MmbId");
        mmbId.appendChild(doc.createTextNode(bankFileXmlDto.getMemberId()));
        clrSysMmbId.appendChild(mmbId);
        finInstnId.appendChild(clrSysMmbId);
        cdtrAgt.appendChild(finInstnId);
        Element brnchId = doc.createElement("BrnchId");
        Element id = doc.createElement("Id");
        brnchId.appendChild(id);
        cdtrAgt.appendChild(brnchId);

        Element cdtr = doc.createElement("Cdtr");
        Element nm = doc.createElement("Nm");
        nm.appendChild(doc.createTextNode(bankFileXmlDto.getName()));
        cdtr.appendChild(nm);

        Element cdtrAcct = doc.createElement("CdtrAcct");
        Element Id = doc.createElement("Id");
        Element othr = doc.createElement("Othr");
        Element tp = doc.createElement("Tp");
        Element prtry = doc.createElement("Prtry");
        prtry.appendChild(doc.createTextNode(bankFileXmlDto.getProprietary()));
        tp.appendChild(prtry);
        cdtrAcct.appendChild(Id);
        cdtrAcct.appendChild(tp);
        Id.appendChild(othr);

        othr.appendChild(id);
        id.appendChild(doc.createTextNode(bankFileXmlDto.getIdentification()));
        Element rmtInf = doc.createElement("RmtInf");
        Element ustrd = doc.createElement("Ustrd");
        ustrd.appendChild(doc.createTextNode(bankFileXmlDto.getUnStructured()));
        rmtInf.appendChild(ustrd);

        cdtTrfTxInf.appendChild(cdtrAgt);
        cdtTrfTxInf.appendChild(cdtr);
        cdtTrfTxInf.appendChild(cdtrAcct);
        cdtTrfTxInf.appendChild(rmtInf);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        return cdtTrfTxInf;
    }
}
