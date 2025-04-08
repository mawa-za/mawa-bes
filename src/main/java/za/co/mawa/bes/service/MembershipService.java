package za.co.mawa.bes.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.controller.BatchController;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.PricingInboundDto;
import za.co.mawa.bes.dto.invoice.InvoiceInboundDto;
import za.co.mawa.bes.dto.invoice.InvoiceOutboundDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.partner.PartnerQueryDto;
import za.co.mawa.bes.dto.premium.PremiumSearchDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemEditDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.entity.transaction.TransactionAmountPKEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.repository.TransactionViewRepository;
import za.co.mawa.bes.utils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MembershipService implements MembershipDao {
    private static final Logger log = LoggerFactory.getLogger(MembershipService.class);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    TransactionViewRepository transactionViewRepository;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    InvoiceService invoiceService;
    @Autowired
    UserService userService;
    @Autowired
    LineItemService lineItemService;

    public String create(MembershipCreateDto membershipCreateDto) throws PartnerNotFoundException, ProductNotFoundException, TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {
        if (partnerService.get(membershipCreateDto.getMemberId()) == null) {
            throw new PartnerNotFoundException("Membership main member does not exist");
        }
        if (productService.get(membershipCreateDto.getProductId()) == null) {
            throw new ProductNotFoundException("Membership product does not exist");
        }
        if (partnerService.get(membershipCreateDto.getSalesRepresentativeId()) == null) {
            throw new PartnerNotFoundException("Membership Sales Representative does not exist");
        }

        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.MEMBERSHIP);
        transactionCreateDto.setSubType(membershipCreateDto.getMembershipType());
        if (Objects.equals(membershipCreateDto.getCreationType(), "TRANSFER")) {
            transactionCreateDto.setStatus(Status.AWAITING_APPROVAL);
            transactionCreateDto.setStatusReason(StatusReason.DOCUMENT_VERIFICATION);
        }
        if (Objects.equals(membershipCreateDto.getCreationType(), "NEW")) {
            if(getWaitingPeriod(membershipCreateDto.getProductId()) > 0){
                transactionCreateDto.setStatus(Status.WAITING_PERIOD);
            }
            else {
                transactionCreateDto.setStatus(Status.NEW);
            }
        }
        if (membershipCreateDto.getCreationType().equals("UPGRADE")){
            int waitingPeriod = getWaitingPeriod(membershipCreateDto.getProductId());
            if(waitingPeriod > 0){
                transactionCreateDto.setStatus(Status.WAITING_PERIOD);
            }
            else {
                transactionCreateDto.setStatus(Status.ACTIVE);
            }
        }
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);
        addEffectiveDate(transactionDto, membershipCreateDto);

        if(membershipCreateDto.getCreationType().equals("UPGRADE")){
            try{
                TransactionLinkDto link = new TransactionLinkDto();
                link.setTransaction1(transactionDto.getId());
                link.setTransaction2(membershipCreateDto.getCurrentMembershipId());
                link.setType(TransactionType.UPGRADE);
                link.setCreateBy(userService.getCurrentUserPartnerId());
                transactionService.addLink(link);
            }
            catch(Exception e){

            }
        }

        ProductDto productDto = productService.get(membershipCreateDto.getProductId());
        TransactionItemDto transactionItemDto = new TransactionItemDto();
        transactionItemDto.setTransaction(transactionDto.getId());
        transactionItemDto.setProduct(membershipCreateDto.getProductId());
        transactionItemDto.setProduct(productDto.getId());
        try {
            ProductPricingQueryDto productPricingQueryDto = new ProductPricingQueryDto();
            productPricingQueryDto.setProduct(membershipCreateDto.getProductId());
            productPricingQueryDto.setPricing(ProductPricing.MONTHLY_PREMIUM);
            ProductPricingDto productPricingDto = productService.getPricing(productPricingQueryDto);
            transactionItemDto.setUnitPrice(productPricingDto.getValue());
        } catch (Exception exception) {

        }
        transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
        transactionItemDto.setQuantity(new BigDecimal("1"));
        transactionService.addItem(transactionItemDto);

        try {
            TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(transactionItemDto.getUnitPrice());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(TransactionAmount.MONTHLY_PREMIUM);
            transactionAmountService.save(transactionAmountInboundDto);
        } catch (Exception exception) {

        }
        if (membershipCreateDto.getDateJoined() != null) {
            TransactionDateDto dateJoined = new TransactionDateDto();
            dateJoined.setTransaction(transactionDto.getId());
            dateJoined.setType(DateType.JOINED);
            dateJoined.setValue(membershipCreateDto.getDateJoined());
            transactionService.addDate(dateJoined);
        } else {
            TransactionDateDto dateJoined = new TransactionDateDto();
            dateJoined.setTransaction(transactionDto.getId());
            dateJoined.setType(DateType.JOINED);
            dateJoined.setValue(new Date());
            transactionService.addDate(dateJoined);
        }
        if (membershipCreateDto.getLastReceiptDate() != null) {
            TransactionDateDto lastReceiptDate = new TransactionDateDto();
            lastReceiptDate.setTransaction(transactionDto.getId());
            lastReceiptDate.setType(DateType.LAST_RECEIPT_DATE);
            lastReceiptDate.setValue(membershipCreateDto.getLastReceiptDate());
            transactionService.addDate(lastReceiptDate);
        }
        if (membershipCreateDto.getMemberId() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.MAINMEMBER);
            transactionPartnerDto.setPartner(membershipCreateDto.getMemberId());
            transactionService.addPartner(transactionPartnerDto);
        }

        if (membershipCreateDto.getSalesRepresentativeId() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
            transactionPartnerDto.setPartner(membershipCreateDto.getSalesRepresentativeId());
            transactionService.addPartner(transactionPartnerDto);
        }

        if (membershipCreateDto.getPreviousInsurerId() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.PREVIOUS_INSURER);
            transactionPartnerDto.setPartner(membershipCreateDto.getPreviousInsurerId());
            transactionService.addPartner(transactionPartnerDto);
        }
        return transactionDto.getId();
    }

    public MembershipDto get(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            MembershipDto membershipDto = new MembershipDto();
            membershipDto.setNumber(transactionDto.getNumber());
            membershipDto.setId(transactionDto.getId());
            membershipDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
            if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                try {
                    membershipDto.setProduct(productService.getBasic(productId));
                } catch (ProductNotFoundException e) {
                }
            }
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                try {
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.MAINMEMBER)) {
                        membershipDto.setMember(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {
                        membershipDto.setSalesRepresentative(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                } catch (PartnerNotFoundException e) {

                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (transactionDateDto.getType().equals(DateType.JOINED)) {
                    membershipDto.setDateJoined(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.EFFECTIVE)) {
                    membershipDto.setDateEffective(transactionDateDto.getValue());
                }
            }

            try {
                membershipDto.setPremium(transactionAmountService.getByTransaction(id).stream()
                        .filter(a -> Objects.equals(a.getType().getCode(), TransactionAmount.MONTHLY_PREMIUM))
                        .toList().iterator().next().getAmount());
            } catch (Exception exception) {
            }
            membershipDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            membershipDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));

            List<TransactionLinkDto> transactionLinkDtos = transactionService.getLinks(id);
            membershipDto.setMembershipHistoryLinks(transactionLinkDtos);

            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.MEMBERSHIP);
            List<MembershipDto> previousMemberships = new ArrayList<>();

            for(TransactionLinkDto link: transactionLinkDtos){
                try{
                    if(link.getType().equalsIgnoreCase("UPGRADE")){
                        previousMemberships.add(get(link.getTransaction2()));
                    }
                }
                catch(Exception e){
                }
            }
            membershipDto.setMembershipHistory(previousMemberships);


            return membershipDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }

    }

    public List<MembershipDto> search(MembershipQueryDto membershipQueryDto) {
        List<MembershipDto> membershipDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();


        transactionQueryDto.setType(TransactionType.MEMBERSHIP);
        transactionQueryDto.setStatus(membershipQueryDto.getStatus());

        for (String id : transactionService.search(transactionQueryDto)) {
            try {
                MembershipDto membershipDto = get(id);

                boolean match = true;

                if(membershipQueryDto.getMemberId() != null) {
                    String memberId = membershipDto.getMember().getId();
                    match =  membershipQueryDto.getMemberId().equals(memberId);
                }

                if(membershipQueryDto.getIdNumber() != null) {
                    String IdNumber = membershipDto.getMember().getIdentity().getNumber();
                    match =  match && membershipQueryDto.getIdNumber().equals(IdNumber);
                }

                if(membershipQueryDto.getProductId() != null){
                    String productId = membershipDto.getProduct().getId();
                    match = match && membershipQueryDto.getProductId().equals(productId);
                }

                if(membershipQueryDto.getSalesRepresentativeId() !=null){
                    String salesRepresentativeId = membershipDto.getSalesRepresentative().getId();
                    match = match && membershipQueryDto.getSalesRepresentativeId().equals(salesRepresentativeId);
                }

                if(membershipQueryDto.getDateJoined() !=null){

                        String dateJoined = Conversion.dateToString(membershipDto.getDateJoined());

                        String queryDateJoined = Conversion.dateToString(membershipQueryDto.getDateJoined());

                        match = match && dateJoined.equals(queryDateJoined);


                }

                if(match) {
                    membershipDtoList.add(membershipDto);
                }

            }catch (Exception e){

            }
        }
        return membershipDtoList;
    }

    @Override
    public void addDependent(DependentDto dependentDto) {

    }

    @Override
    public void removeDependent(DependentDto dependentDto) {

    }

    public Boolean edit(String id, MembershipEditDto membershipDto) {
        boolean edited = false;
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            TransactionPartnerEdit partnerEdit = new TransactionPartnerEdit();

            if (membershipDto.getStatus() != null && membershipDto.getStatus() != "") {
                transactionEditDto.setStatus(membershipDto.getStatus());
            }
            if (membershipDto.getStatusReason() != null && membershipDto.getStatusReason() != "") {
                transactionEditDto.setStatusReason(membershipDto.getStatusReason());
            }
            if (transactionEditDto.getStatusReason() != null || transactionEditDto.getStatus() != null) {
                transactionEditDto.setId(id);
                transactionService.edit(transactionEditDto);
            }
            if (membershipDto.getSalesRepresentativeId() != null && membershipDto.getSalesRepresentativeId() != "") {
                partnerEdit.setPartnerFunction(PartnerFunction.SALES_REPRESENTATIVE);
                partnerEdit.setTransaction(id);
                partnerEdit.setParnter(membershipDto.getSalesRepresentativeId());
                edited = transactionService.partnerEdit(partnerEdit);
            }
            if (membershipDto.getPremium() != null && membershipDto.getProductId() != null && membershipDto.getProductId() != "") {
                TransactionItemEditDto editDto = new TransactionItemEditDto();
                editDto.setTransaction(id);
                editDto.setProduct(membershipDto.getProductId());
                editDto.setUnitPrice(membershipDto.getPremium());
                edited = transactionService.editItem(editDto);
            }
            if (membershipDto.getProductId() != null && membershipDto.getProductId() != "" && membershipDto.getPreviousProduct() != null && membershipDto.getPreviousProduct() != "") {
                TransactionItemEditDto editDto = new TransactionItemEditDto();
                editDto.setTransaction(id);
                editDto.setProduct(membershipDto.getProductId());
                editDto.setPreviousProduct(membershipDto.getPreviousProduct());
                edited = transactionService.editItem(editDto);
            }
            return edited;
        }
        catch (Exception e){
            return edited;
        }
    }

    public List<MembershipDto> getByFilter(MembershipQueryDto membershipQueryDto) throws Exception {
        List<MembershipDto> membershipDtoList = new ArrayList<>();
        List<String> transactionList = new ArrayList<>();
        if (membershipQueryDto.getPartnerFunction() != null || membershipQueryDto.getMemberId() != null) {

            List<TransactionPartnerDto> transactionPartnerDtos = transactionService.getPartnersByFunction(PartnerFunction.MAINMEMBER);

            if (!transactionPartnerDtos.isEmpty()) {
                for (TransactionPartnerDto transactionPartnerDto : transactionPartnerDtos) {
                    String transaction = "";

                    if (membershipQueryDto.getMemberId() != null) {
                        if (transactionPartnerDto.getPartner().equals(membershipQueryDto.getMemberId())) {
                            transaction = transactionPartnerDto.getTransaction();
                        }
                    } else {
                        transaction = transactionPartnerDto.getTransaction();
                    }

                    transactionList.add(transaction);

                }

            }

        }

        List<TransactionItemDto> transactionItemDtos = new ArrayList<>();
        if (!transactionList.isEmpty() && membershipQueryDto.getProductId() != null) {

            Iterator<String> iterator = transactionList.iterator();

            while (iterator.hasNext()) {
                String transaction = iterator.next();
                transactionItemDtos = transactionService.getItems(transaction);

                if (!transactionItemDtos.isEmpty()) {
                    for (TransactionItemDto transactionItemDto : transactionItemDtos) {
                        if (!transactionItemDto.getProduct().equals(membershipQueryDto.getProductId())) {
                            iterator.remove();
                        }
                    }
                }
            }

        } else if (membershipQueryDto.getProductId() != null) {

            Iterator<String> iterator = transactionList.iterator();
            TransactionItemDto transactionItemDto = new TransactionItemDto();
            transactionItemDto.setProduct(membershipQueryDto.getProductId());
            List<TransactionItemDto> transactionItemDtoList = transactionService.getItemsBy(transactionItemDto);


            if (!transactionItemDtoList.isEmpty()) {
                for (TransactionItemDto transactionItemDto1 : transactionItemDtoList) {
                    while (iterator.hasNext()) {
                        String transaction = iterator.next();
                        if (!transaction.equals(transactionItemDto1.getTransaction())) {
                            iterator.remove();
                        }

                    }

                }

            }
        }


        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();

        transactionQueryDto.setType(TransactionType.MEMBERSHIP);
        for (String id : transactionService.search(transactionQueryDto)) {

            if (transactionList.isEmpty()) {
                MembershipDto membershipDto = get(id);
                if (membershipQueryDto.getStatus() != null) {
                    if (membershipDto.getStatus().getCode().equals(membershipQueryDto.getStatus())) {
                        membershipDtoList.add(membershipDto);
                    }

                } else {
                    membershipDtoList.add(membershipDto);
                }

            } else {
                Iterator<String> iterator = transactionList.iterator();
                while (iterator.hasNext()) {
                    String transaction = iterator.next();
                    if (transaction.equals(id)) {
                        MembershipDto membershipDto = get(transaction);

                        if (membershipQueryDto.getStatus() != null) {
                            if (membershipDto.getStatus().getCode().equals(membershipQueryDto.getStatus())) {
                                membershipDtoList.add(membershipDto);
                            }

                        } else {
                            membershipDtoList.add(membershipDto);
                        }
                    }
                }

            }

        }

        return membershipDtoList;
    }

    public String handleMembershipLapse(String id) throws Exception {
        PremiumSearchDto premiumSearchDto = new PremiumSearchDto();
        premiumSearchDto.setMembershipId(id);
        List<PremiumEntity> premiumEntities = transactionService.search(premiumSearchDto);
        return processMembershipLapseLogic(premiumEntities, id);
    }

    public String handleMembershipLapse(List<TransactionViewEntity> membershipEntities) throws Exception {
        PremiumSearchDto premiumSearchDto = new PremiumSearchDto();
        List<PremiumEntity> premiumEntities = transactionService.search(premiumSearchDto);
        for (TransactionViewEntity entity : membershipEntities) {
            processMembershipLapseLogic(premiumEntities, entity.getTransactionId());
        }
        return "Membership Lapse Finished";
    }

    private String processMembershipLapseLogic(List<PremiumEntity> premiumEntities, String membershipId) {
        LocalDate today = LocalDate.now();
        LocalDate threeMonthsAgo = today.minusMonths(3);

        for (PremiumEntity premiumEntity : premiumEntities) {
            if (premiumEntity != null && membershipId.equals(premiumEntity.getMembershipId())) {
                LocalDate localDateToCheck = premiumEntity.getCreationDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (localDateToCheck.isBefore(threeMonthsAgo)) {
                    MembershipEditDto editDto = new MembershipEditDto();
                    editDto.setStatus(Status.INACTIVE);
                    editDto.setStatusReason(StatusReason.LAPSED);
                    edit(membershipId, editDto);
                }
            }
        }
        return "Processed";
    }

    public String handleBilling(String id){
        try {
            MembershipDto membershipDto = get(id);
            InvoiceInboundDto invoiceInboundDto = new InvoiceInboundDto();

            if(membershipDto.getMember()!= null){
                invoiceInboundDto.setCustomerId(membershipDto.getMember().getId());
            }
            if(membershipDto.getSalesRepresentative() != null){
                invoiceInboundDto.setSalesRepresentative(membershipDto.getSalesRepresentative().getId());
            }
            PricingInboundDto pricingInboundDto = new PricingInboundDto();
            if(membershipDto.getPremium() != null){
                pricingInboundDto.setTotalIncVat(membershipDto.getPremium());
            }
            invoiceInboundDto.setPricing(pricingInboundDto);
            invoiceInboundDto.setInvoiceDate(new Date());

            List<LineItemInboundDto> lineItemInboundDtoList = new ArrayList<>();
            LineItemInboundDto lineItemInboundDto = new LineItemInboundDto();
            lineItemInboundDto.setQuantity(BigDecimal.valueOf(1));
            if(membershipDto.getPremium() != null && membershipDto.getProduct() != null){
                lineItemInboundDto.setUnitPrice(membershipDto.getPremium());
                lineItemInboundDto.setProductId(membershipDto.getProduct().getId());
            }
            lineItemInboundDtoList.add(lineItemInboundDto);
            invoiceInboundDto.setItems(lineItemInboundDtoList);
            invoiceInboundDto.setTransactionSubType(InvoiceType.MEMBERSHIP);
            invoiceInboundDto.setInvoiceType(InvoiceType.MEMBERSHIP);

            String invoiceId = invoiceService.create(invoiceInboundDto);

            TransactionLinkDto linkDto = new TransactionLinkDto();
            linkDto.setTransaction1(invoiceId);
            linkDto.setTransaction2(id);
            linkDto.setType(TransactionType.MEMBERSHIP);
            transactionService.addLink(linkDto);

            linkDto = new TransactionLinkDto();
            linkDto.setTransaction1(id);
            linkDto.setTransaction2(invoiceId);
            linkDto.setType(TransactionType.INVOICE);
            transactionService.addLink(linkDto);

            return invoiceId;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String validateMemberships() throws Exception {
        TransactionViewDto transactionViewDto = new TransactionViewDto();
        transactionViewDto.setType(TransactionType.MEMBERSHIP);
        try{
            List<TransactionViewEntity> entities = transactionService.searchV2(transactionViewDto);

            Set<TransactionViewEntity> uniqueEntities = new HashSet<>(entities);
            MembershipEditDto editDto = new MembershipEditDto();

            for (TransactionViewEntity entity : uniqueEntities) {
                if(entity.getTransactionStatus().equalsIgnoreCase(Status.AWAITING_APPROVAL)){
                    continue;
                }
                if (entity.getDateEffective() != null) {
                    LocalDateTime effectiveDateTime = LocalDateTime.parse(entity.getDateEffective(), formatter);
                    LocalDate effectiveDate = effectiveDateTime.toLocalDate();
                    LocalDate today = LocalDate.now();

                    PremiumSearchDto searchDto = new PremiumSearchDto();
                    searchDto.setMembershipId(entity.getTransactionId());
                    List<PremiumEntity> premiumEntities = transactionService.search(searchDto);
                    if(premiumEntities != null){
                        for(PremiumEntity premiumEntity: premiumEntities){
                            int waitingPeriod = getWaitingPeriod(entity.getProductId());
                            MembershipDto membershipDto = new MembershipDto();
                            if(waitingPeriod > 0){
                                try{
                                    membershipDto = get(entity.getTransactionId());
                                }
                                catch(Exception e){

                                }
                                LocalDate targetDate = LocalDateTime.parse(
                                        Conversion.dateTimeToString(premiumEntity.getCreationDate()),
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                ).toLocalDate();
                                LocalDate startDate = LocalDateTime.parse(
                                        Conversion.dateTimeToString(membershipDto.getDateJoined()),
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                ).toLocalDate();

                                boolean isWithinRange = isDateWithinRange(targetDate, startDate, effectiveDate);
                                if(isWithinRange){
                                    editDto.setStatus(Status.WAITING_PERIOD);
                                }
                                else if(effectiveDate.isAfter(today)){
                                    editDto.setStatus(Status.ACTIVE);
                                }
                            }
                            else{
                                editDto.setStatus(Status.ACTIVE);
                            }
                            edit(entity.getTransactionId(), editDto);
                        }
                    }
                }
            }
            return "Validated";

        }catch(Exception e){
            throw  new Exception(e.getMessage());
        }
    }

    private static Date addDaysToDate(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private int getWaitingPeriod(String productId) {
        List<ProductAttributeDto> productAttributes = productService.getAttributes(productId);
        return productAttributes.stream()
                .filter(attr -> attr.getAttribute().getCode().equalsIgnoreCase(Status.WAITING_PERIOD))
                .findFirst()
                .map(attr -> Integer.parseInt(attr.getValue()))
                .orElse(0);
    }

    private void addEffectiveDate(TransactionDto transactionDto, MembershipCreateDto membershipCreateDto) throws TransactionDateAddException {
        int waitingPeriod = getWaitingPeriod(membershipCreateDto.getProductId());

        LocalDate today = LocalDate.now();
        Date date = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        TransactionDateDto dateEffective = new TransactionDateDto();
        dateEffective.setTransaction(transactionDto.getId());
        dateEffective.setType(DateType.EFFECTIVE);

        if (Objects.equals(membershipCreateDto.getCreationType(), "TRANSFER")) {
            dateEffective.setValue(date);
        }
        if (Objects.equals(membershipCreateDto.getCreationType(), "NEW") || Objects.equals(membershipCreateDto.getCreationType(), "UPGRADE")) {
            dateEffective.setValue(addDaysToDate(date, waitingPeriod));
            if(membershipCreateDto.getDateJoined() !=  null){
                if(waitingPeriod > 0){
                    dateEffective.setValue(addDaysToDate(membershipCreateDto.getDateJoined(), waitingPeriod));
                }
                else{
                    dateEffective.setValue(addDaysToDate(date, waitingPeriod));
                }
            }
            else {
                dateEffective.setValue(addDaysToDate(date, waitingPeriod));
            }
        }
        transactionService.addDate(dateEffective);
    }

    private static boolean isDateWithinRange(LocalDate targetDate, LocalDate startDate, LocalDate endDate) {
        // Check if targetDate is between startDate and endDate (inclusive)
        return (targetDate.isEqual(startDate) || targetDate.isAfter(startDate)) &&
                (targetDate.isEqual(endDate) || targetDate.isBefore(endDate));
    }
}
