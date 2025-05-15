package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.invoice.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.ByteArrayResource;
import za.co.mawa.bes.dto.partner.PartnerDto;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class InvoiceService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PricingService pricingService;
    @Autowired
    LineItemService lineItemService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    TransactionAttributeService transactionAttributeService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    UserService userService;
    @Autowired
    TransactionRepository transactionRepository;

    public String create(InvoiceInboundDto invoiceInboundDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.INVOICE);

            transactionCreateDto.setCreatedBy(userService.getCurrentUser());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            if (invoiceInboundDto.getInvoiceDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.INVOICE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getInvoiceDate());
                transactionService.addDate(transactionDateDto);
            }
            if (invoiceInboundDto.getDueDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.DUE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getDueDate());
                transactionService.addDate(transactionDateDto);
            }

            if (invoiceInboundDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(invoiceInboundDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (invoiceInboundDto.getSalesRepresentative() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
                transactionPartnerDto.setPartner(invoiceInboundDto.getSalesRepresentative());
                transactionService.addPartner(transactionPartnerDto);
            }
            try{
                for (LineItemInboundDto lineItemInboundDto : invoiceInboundDto.getItems()) {
                    lineItemInboundDto.setTransaction(transactionDto.getId());
                    lineItemService.add(lineItemInboundDto);
                }
            }catch (Exception e){
            }

            try{
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountAmount());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(TransactionAmount.DISCOUNT_AMOUNT);
                transactionAmountService.save(transactionAmountInboundDto);

                transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATAmount());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(TransactionAmount.VAT_AMOUNT);
                transactionAmountService.save(transactionAmountInboundDto);

                transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalIncVat());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.TOTAL_INC_VAT);
                transactionAmountService.save(transactionAmountInboundDto);

                transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalExcVat());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.TOTAL_EXC_VAT);
                transactionAmountService.save(transactionAmountInboundDto);

                transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATPercentage());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.VAT_PERCENTAGE);
                transactionAmountService.save(transactionAmountInboundDto);

                transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountPercentage());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(AmountType.DISCOUNT_PERCENTAGE);
                transactionAmountService.save(transactionAmountInboundDto);
            }catch(Exception e){

            }



            if (invoiceInboundDto.getTransactionSubType() != null && !invoiceInboundDto.getTransactionSubType().isEmpty()){
                try {
                    TransactionLinkDto link = new TransactionLinkDto();

                    if(invoiceInboundDto.getTransactionSubType().equalsIgnoreCase("APPOINTMENT")){
                        link.setTransaction1(transactionDto.getId());
                        link.setTransaction2(invoiceInboundDto.getTransactionSubType());
                        link.setType(TransactionType.APPOINTMENT);
                        link.setCreateBy(userService.getCurrentUser());
                        transactionService.addLink(link);
                    }
                    if(invoiceInboundDto.getTransactionSubType().equalsIgnoreCase("SALES-INVOICE")){
                        link.setTransaction1(transactionDto.getId());
                        link.setTransaction2(invoiceInboundDto.getTransactionSubType());
                        link.setType(TransactionType.SALES_INVOICE);
                        link.setCreateBy(userService.getCurrentUser());
                        transactionService.addLink(link);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return transactionDto.getId();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public InvoiceOutboundDto get(String id) {
        InvoiceOutboundDto invoiceOutboundDto = new InvoiceOutboundDto();
        try {
            TransactionDto transactionDto = transactionService.get(id);
            invoiceOutboundDto.setId(transactionDto.getId());
            invoiceOutboundDto.setNumber(transactionDto.getNumber());
            invoiceOutboundDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            invoiceOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            invoiceOutboundDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));

            invoiceOutboundDto.setInvoiceType(fieldOptionService.getFieldOption(Field.INVOICE_TYPE, transactionDto.getSubType()));

            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(transactionDto.getId());
            transactionAttributeDto.setAttribute(TransactionAttribute.PAYMENT_METHOD);
            invoiceOutboundDto.setPaymentTerms(fieldOptionService.getFieldOption(Field.PAYMENT_TERMS, transactionAttributeService.get(transactionAttributeDto)));

            try {
                for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CUSTOMER)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            invoiceOutboundDto.setCustomer((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.SALES_REPRESENTATIVE)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            invoiceOutboundDto.setSalesRepresentative((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                }
            } catch (Exception e) {
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (Objects.equals(transactionDateDto.getType(), DateType.INVOICE_DATE)) {
                    invoiceOutboundDto.setInvoiceDate(transactionDateDto.getValue());
                }
                if (Objects.equals(transactionDateDto.getType(), DateType.DUE_DATE)) {
                    invoiceOutboundDto.setDueDate(transactionDateDto.getValue());
                }
            }
            invoiceOutboundDto.setItems(lineItemService.getAll(id));
            invoiceOutboundDto.setAmounts(transactionAmountService.getByTransaction(id));
            invoiceOutboundDto.setDates(transactionService.getDates(id));
            try{
                invoiceOutboundDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
            }
            catch (Exception e){
            }
            List<TransactionLinkDto> links = transactionService.getLinks(id);
            for(TransactionLinkDto link : links){
                if(link.getType().equals(TransactionType.APPOINTMENT)){
                    TransactionEntity transaction = transactionRepository.getById(link.getTransaction2());
                    invoiceOutboundDto.setSubTransactionId(transaction.getId());
                    invoiceOutboundDto.setInvoiceType(fieldOptionService.getFieldOption(Field.INVOICE_TYPE, InvoiceType.APPOINTMENT));
                }
                if(link.getType().equals(TransactionType.SALES_INVOICE)){
                    TransactionEntity transaction = transactionRepository.getById(link.getTransaction2());
                    invoiceOutboundDto.setSubTransactionId(transaction.getId());
                    invoiceOutboundDto.setInvoiceType(fieldOptionService.getFieldOption(Field.INVOICE_TYPE, InvoiceType.SALES_INVOICE));
                }
            }
            for(TransactionLinkDto link:transactionService.getLinks(id)){
                if(link.getType().equalsIgnoreCase(TransactionType.MEMBERSHIP)){
                    invoiceOutboundDto.setSubTransactionId(link.getTransaction2());
                }
            }
        } catch (TransactionNotFound exception) {
        }
        return invoiceOutboundDto;
    }

    public List<InvoiceOutboundDto> search(InvoiceQueryDto invoiceQueryDto) {
        List<InvoiceOutboundDto> invoiceOutboundDtoList = new ArrayList<>();
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            if (invoiceQueryDto.getStatus() != null && invoiceQueryDto.getStatus() != "") {
                transactionQueryDto.setStatus(invoiceQueryDto.getStatus());
            }
            if (invoiceQueryDto.getNumber() != null && invoiceQueryDto.getNumber() != "") {
                transactionQueryDto.setNumber(invoiceQueryDto.getNumber());
            }
            if (invoiceQueryDto.getCustomer() != null & invoiceQueryDto.getCustomer() != "") {
                transactionQueryDto.setPartnerNo(invoiceQueryDto.getCustomer());
                transactionQueryDto.setPartnerFunction(PartnerFunction.CUSTOMER);
            }
            transactionQueryDto.setType(TransactionType.INVOICE);
            for (String id : transactionService.search(transactionQueryDto)) {
                try {
                    invoiceOutboundDtoList.add(get(id));
                } catch (Exception exception) {
                }
            }
        } catch (Exception exception) {
        }
        return invoiceOutboundDtoList;
    }

    public InvoiceOutboundDto edit(String id, InvoiceInboundDto invoiceInboundDto) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if (transactionDto == null) {
                throw new RuntimeException("Invoice not found with ID: " + id);
            }
            if (invoiceInboundDto.getInvoiceDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.INVOICE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getInvoiceDate());
                transactionService.editDate(transactionDateDto);
            }
            if (invoiceInboundDto.getDueDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.DUE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getDueDate());
                transactionService.editDate(transactionDateDto);
            }
            if (invoiceInboundDto.getCustomerId() != null) {
                TransactionPartnerEdit transactionPartnerDto = new TransactionPartnerEdit();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setPartnerFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setParnter(invoiceInboundDto.getCustomerId());
                transactionService.partnerEdit(transactionPartnerDto);
            }
            if (invoiceInboundDto.getSalesRepresentative() != null) {
                TransactionPartnerEdit transactionPartnerDto = new TransactionPartnerEdit();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setPartnerFunction(PartnerFunction.SALES_REPRESENTATIVE);
                transactionPartnerDto.setParnter(invoiceInboundDto.getSalesRepresentative());
                transactionService.partnerEdit(transactionPartnerDto);
            }
            lineItemService.delete(transactionDto.getId()); // remove existing items
            for (LineItemInboundDto lineItemInboundDto : invoiceInboundDto.getItems()) {
                lineItemInboundDto.setTransaction(transactionDto.getId());
                lineItemService.add(lineItemInboundDto);
            }
            TransactionAmountInboundDto transactionAmountInboundDto  = new TransactionAmountInboundDto();;
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountAmount());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(TransactionAmount.DISCOUNT_AMOUNT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATAmount());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(TransactionAmount.VAT_AMOUNT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalIncVat());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.TOTAL_INC_VAT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalExcVat());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.TOTAL_EXC_VAT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATPercentage());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.VAT_PERCENTAGE);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountPercentage());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.DISCOUNT_PERCENTAGE);
            transactionAmountService.save(transactionAmountInboundDto);

            return get(transactionDto.getId());
        } catch (Exception exception) {
            throw new RuntimeException("Error updating invoice: " + exception.getMessage(), exception);
        }
    }

    public List<TransactionViewEntity> getMembershipInvoices(String id){
        try{
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.INVOICE);
            List<TransactionViewEntity> transactionViewEntities = transactionService.searchV2(transactionViewDto);
            List<TransactionLinkDto> transactionLinkDtos = transactionService.getLinks(id);
            List<TransactionViewEntity> invoices = new ArrayList<>();

            for(TransactionLinkDto link: transactionLinkDtos){
                if(link.getType().equalsIgnoreCase("INVOICE")){
                    for(TransactionViewEntity entity : transactionViewEntities){
                        if(Objects.equals(entity.getTransactionId(), link.getTransaction2())){
                            invoices.add(entity);
                        }
                    }
                }
            }
            return invoices;
        }
        catch(Exception ex){
            throw new RuntimeException("No invoices found");
        }
    }

    public ByteArrayResource generateInvoice(InvoiceOutboundDto invoiceOutboundDto) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float margin = 50;
                float yStart = page.getMediaBox().getHeight() - margin;
                float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
                float yPosition = yStart;

                // Title
                contentStream.setFont(fontBold, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("INVOICE");
                contentStream.endText();
                yPosition -= 30;

                // Invoice Details
                contentStream.setFont(fontRegular, 12);
                drawText(contentStream, "Invoice Number: " + invoiceOutboundDto.getNumber(), margin, yPosition);
                yPosition -= 20;
                drawText(contentStream, "Invoice Date: " + formatDate(invoiceOutboundDto.getInvoiceDate()), margin, yPosition);
                yPosition -= 20;

                // Customer Information
                PartnerDto customer = invoiceOutboundDto.getCustomer();
                if (customer != null) {
                    drawText(contentStream, "Customer: " + customer.getName1() + " " + customer.getName2(), margin, yPosition);
                    yPosition -= 20;
                }

                // Sales Representative Information
                PartnerDto salesRepresentative = invoiceOutboundDto.getSalesRepresentative();
                if (salesRepresentative != null) {
                    drawText(contentStream, "Sales Rep: " + salesRepresentative.getName1() + " " + salesRepresentative.getName2(), margin, yPosition);
                    yPosition -= 20;
                }

                // Line Items Table
                List<LineItemOutboundDto> lineItems = invoiceOutboundDto.getItems();
                if (lineItems != null && !lineItems.isEmpty()) {
                    yPosition -= 30;
                    contentStream.setFont(fontBold, 14);
                    drawText(contentStream, "Line Items", margin, yPosition);
                    yPosition -= 20;

                    float rowHeight = 20;
                    float colWidth = tableWidth / 4;
                    float tableYStart = yPosition;

                    // Table Headers
                    contentStream.setFont(fontBold, 12);
                    drawTableRow(contentStream, margin, tableYStart, colWidth, rowHeight, "Product", "Quantity", "Unit Price", "Total");
                    yPosition -= rowHeight;
                    contentStream.setFont(fontRegular, 12);

                    // Table Data
                    for (LineItemOutboundDto lineItem : lineItems) {
                        drawTableRow(contentStream, margin, yPosition, colWidth, rowHeight,
                                String.valueOf(lineItem.getProduct().getCode()),
                                String.valueOf(lineItem.getQuantity()),
                                String.format("%.2f", lineItem.getUnitPrice()),
                                String.format("%.2f", lineItem.getUnitPrice()));
                        yPosition -= rowHeight;
                    }
                }

                // Total Amount
                invoiceOutboundDto.setItems(lineItemService.getAll(invoiceOutboundDto.getId()));
                invoiceOutboundDto.setAmounts(transactionAmountService.getByTransaction(invoiceOutboundDto.getId()));
                if (invoiceOutboundDto.getAmounts() != null) {
                    yPosition -= 30;
                    contentStream.setFont(fontBold, 14);
//                    drawText(contentStream, "Total Amount: " + "String.format("%.2f", "invoiceOutboundDto.getAmounts()")", margin, yPosition);
                }
            }

            // Save and return
            document.save(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void drawText(PDPageContentStream contentStream, String text, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private void drawTableRow(PDPageContentStream contentStream, float x, float y, float colWidth, float rowHeight,
                              String col1, String col2, String col3, String col4) throws IOException {
        drawText(contentStream, col1, x, y);
        drawText(contentStream, col2, x + colWidth, y);
        drawText(contentStream, col3, x + 2 * colWidth, y);
        drawText(contentStream, col4, x + 3 * colWidth, y);
    }


    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}