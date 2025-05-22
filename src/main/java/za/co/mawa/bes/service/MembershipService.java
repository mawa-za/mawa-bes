package za.co.mawa.bes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.PricingInboundDto;
import za.co.mawa.bes.dto.invoice.InvoiceInboundDto;
import za.co.mawa.bes.dto.invoice.InvoiceOutboundDto;
import za.co.mawa.bes.dto.membership.*;
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
import za.co.mawa.bes.entity.transaction.TransactionDateEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.repository.TransactionDateRepository;
import za.co.mawa.bes.repository.TransactionViewRepository;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MembershipService implements MembershipDao {
    private static final Logger log = LoggerFactory.getLogger(MembershipService.class);
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
    TransactionDateRepository transactionDateRepository;

    public MembershipDto create(MembershipCreateDto membershipCreateDto) throws PartnerNotFoundException, ProductNotFoundException, TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {
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
        TransactionItemDto transactionItemDto = new TransactionItemDto();

        transactionCreateDto.setType(TransactionType.MEMBERSHIP);
        transactionCreateDto.setSubType(membershipCreateDto.getMembershipType());

        if (Objects.equals(membershipCreateDto.getCreationType(), "TRANSFER")) {
            transactionCreateDto.setStatus(Status.AWAITING_APPROVAL);
            transactionItemDto.setStatus(Status.AWAITING_APPROVAL);
            transactionCreateDto.setStatusReason(StatusReason.DOCUMENT_VERIFICATION);
        }

        if (Objects.equals(membershipCreateDto.getCreationType(), "NEW")) {
            int waitingPeriod = getWaitingPeriod(membershipCreateDto.getProductId(), Status.WAITING_PERIOD);

            if (waitingPeriod > 0) {
                Date effectiveDate = addDaysToDate(membershipCreateDto.getDateJoined(), waitingPeriod);
                if (effectiveDate.before(new Date())) {
                    transactionCreateDto.setStatus(Status.ACTIVE);
                    transactionItemDto.setStatus(Status.ACTIVE);
                } else {
                    transactionCreateDto.setStatus(Status.WAITING_PERIOD);
                    transactionItemDto.setStatus(Status.WAITING_PERIOD);
                }
            } else {
                if (membershipCreateDto.getDateJoined().before(new Date())) {
                    transactionCreateDto.setStatus(Status.ACTIVE);
                    transactionItemDto.setStatus(Status.ACTIVE);
                } else {
                    transactionCreateDto.setStatus(Status.NEW);
                    transactionItemDto.setStatus(Status.NEW);
                }
            }
        }

        String existingMembershipId = null;
        TransactionItemDto previousActiveItem = null;
        boolean hasUpgradeWaitingPeriod = false;

        if (membershipCreateDto.getCreationType().equalsIgnoreCase("UPGRADE")) {
            try {
                existingMembershipId = membershipCreateDto.getCurrentMembershipId();
                int upgradeWaitingPeriod = getWaitingPeriod(membershipCreateDto.getProductId(), Status.UPGRADE_WAITING_PERIOD);

                hasUpgradeWaitingPeriod = upgradeWaitingPeriod > 0;

                if (hasUpgradeWaitingPeriod) {
                    transactionCreateDto.setStatus(Status.UPGRADE_WAITING_PERIOD);
                    transactionItemDto.setStatus(Status.UPGRADE_WAITING_PERIOD);
                    log.info("Upgrade has waiting period of {} days", upgradeWaitingPeriod);
                } else {
                    transactionCreateDto.setStatus(Status.ACTIVE);
                    transactionItemDto.setStatus(Status.ACTIVE);
                    log.info("Upgrade has no waiting period, will be active immediately");
                }

                // Validate current membership and get latest active item
                previousActiveItem = transactionService
                        .getItems(existingMembershipId)
                        .stream()
                        .filter(item -> item.getStatus() == null ||
                                !item.getStatus().equalsIgnoreCase(Status.INACTIVE))
                        .max(Comparator.comparing(TransactionItemDto::getValidFrom))
                        .orElse(null);

                if (previousActiveItem == null) {
                    throw new RuntimeException("No active item found for current membership");
                }

                // Check if upgrade is allowed
                if (previousActiveItem.getStatus() != null) {
                    if (previousActiveItem.getStatus().equalsIgnoreCase(Status.UPGRADE_WAITING_PERIOD)) {
                        throw new RuntimeException("Membership upgrade is not allowed while you are in the upgrade waiting period.");
                    }
                    if (previousActiveItem.getStatus().equalsIgnoreCase(Status.WAITING_PERIOD)) {
                        throw new RuntimeException("Membership upgrade is not allowed while you are in the waiting period.");
                    }
                    if (previousActiveItem.getStatus().equalsIgnoreCase(Status.AWAITING_APPROVAL)) {
                        throw new RuntimeException("Cannot upgrade while waiting for approval");
                    }
                }

                if (!hasUpgradeWaitingPeriod) {
                    TransactionItemEditDto itemEditDto = new TransactionItemEditDto();
                    itemEditDto.setTransaction(existingMembershipId);
                    itemEditDto.setItem(previousActiveItem.getItem());
                    itemEditDto.setProduct(previousActiveItem.getProduct());
                    itemEditDto.setStatus(Status.INACTIVE);
                    itemEditDto.setValidTo(new Date());
                    transactionService.editItem(itemEditDto);
                    log.info("Inactivated previous item immediately (no upgrade waiting period)");
                } else {
                    // Keep the previous item active during upgrade waiting period
                    log.info("Keeping previous item active during upgrade waiting period");
                }

            } catch (Exception e) {
                throw new RuntimeException("Error during upgrade process: " + e.getMessage(), e);
            }
        }

        TransactionDto transactionDto;
        if (!membershipCreateDto.getCreationType().equalsIgnoreCase("UPGRADE")) {
            transactionDto = transactionService.create(transactionCreateDto);
        } else {
            transactionDto = new TransactionDto();
            transactionDto.setId(existingMembershipId);


            if (hasUpgradeWaitingPeriod && previousActiveItem != null) {
                transactionCreateDto.setStatus(previousActiveItem.getStatus());
            }
            try{
                TransactionEditDto transactionEditDto = new TransactionEditDto();
                transactionEditDto.setId(existingMembershipId);
                transactionEditDto.setStatus(transactionCreateDto.getStatus());
                transactionService.edit(transactionEditDto);
            }catch(Exception e){

            }
        }

        ProductDto productDto = productService.get(membershipCreateDto.getProductId());
        transactionItemDto.setTransaction(transactionDto.getId());
        transactionItemDto.setProduct(productDto.getId());

        try {
            ProductPricingQueryDto productPricingQueryDto = new ProductPricingQueryDto();
            productPricingQueryDto.setProduct(membershipCreateDto.getProductId());
            productPricingQueryDto.setPricing(ProductPricing.MONTHLY_PREMIUM);
            ProductPricingDto productPricingDto = productService.getPricing(productPricingQueryDto);
            transactionItemDto.setUnitPrice(productPricingDto.getValue());
        } catch (Exception exception) {
            log.warn("Could not retrieve product pricing: {}", exception.getMessage());
        }

        try {
            addEffectiveDate(transactionDto, membershipCreateDto);
            TransactionDateEntity entity = transactionDateRepository.getTransactionDatesType(transactionDto.getId(), DateType.EFFECTIVE);
            if (entity != null) {
                transactionItemDto.setValidTo(entity.getValue());
            }
        } catch (Exception e) {
            log.warn("Could not add effective date: {}", e.getMessage());
        }

        transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
        transactionItemDto.setQuantity(new BigDecimal("1"));

        if (membershipCreateDto.getCreationType().equalsIgnoreCase("NEW")) {
            transactionItemDto.setValidFrom(membershipCreateDto.getDateJoined());
        } else {
            transactionItemDto.setValidFrom(new Date());
        }

        transactionService.addItem(transactionItemDto);

        try {
            if (transactionItemDto.getUnitPrice() != null) {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(transactionItemDto.getUnitPrice());
                transactionAmountInboundDto.setTransaction(transactionDto.getId());
                transactionAmountInboundDto.setType(TransactionAmount.MONTHLY_PREMIUM);
                transactionAmountService.save(transactionAmountInboundDto);
            }
        } catch (Exception exception) {
            log.warn("Could not save transaction amount: {}", exception.getMessage());
        }

        try {
            if (membershipCreateDto.getCreationType().equalsIgnoreCase("UPGRADE") && hasUpgradeWaitingPeriod) {
                enforceUpgradeWaitingPeriodRules(transactionDto, membershipCreateDto.getProductId(), previousActiveItem);
            } else {
                enforceProductStatusRules(transactionDto);
            }
        } catch (Exception e) {
            log.warn("Could not enforce product status rules: " + e.getMessage());
        }

        if (!membershipCreateDto.getCreationType().equalsIgnoreCase("UPGRADE")) {
            TransactionDateDto dateJoined = new TransactionDateDto();
            dateJoined.setTransaction(transactionDto.getId());
            dateJoined.setType(DateType.JOINED);
            dateJoined.setValue(membershipCreateDto.getDateJoined() != null ?
                    membershipCreateDto.getDateJoined() : new Date());
            transactionService.addDate(dateJoined);

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
        }

        try {
            if (membershipCreateDto.getCreationType().equalsIgnoreCase("UPGRADE") && hasUpgradeWaitingPeriod) {
                updateMembershipFromSpecificItem(transactionDto.getId(), previousActiveItem);
                log.info("Updated membership to reflect previous active item during upgrade waiting period");
            } else {
                updateMembershipFromLatestItem(transactionDto.getId());
                log.info("Successfully created/updated membership: {} with type: {}",
                        transactionDto.getId(), membershipCreateDto.getCreationType());
            }
        } catch (Exception e) {
            log.warn("Could not update membership status: {}", e.getMessage());
        }

        return get(transactionDto.getId());
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

            try{

            }catch(Exception e){

            }
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

    public String scheduledStatusChange() {
        try{
            processTenantTransactions();
            return "Scheduling Finished";
        }
        catch (Exception e) {

            System.err.println("Error during scheduled status change: " + e.getMessage());
        }
        return "Scheduling Error Occurred";
    }

    private void processTenantTransactions() throws Exception {
        PremiumSearchDto premiumSearchDto = new PremiumSearchDto();
        TransactionViewDto transactionViewDto = new TransactionViewDto();
        transactionViewDto.setType(TransactionType.MEMBERSHIP);

        try {
            List<TransactionViewEntity> membershipEntities = transactionService.searchV2(transactionViewDto);
            List<PremiumEntity> premiumEntities = transactionService.search(premiumSearchDto);


            LocalDate today = LocalDate.now();
            LocalDate threeMonthsAgo = today.minusMonths(3);

            for (TransactionViewEntity entity : membershipEntities) {
                if (!premiumEntities.isEmpty()) {
                    for (PremiumEntity premiumEntity : premiumEntities) {
                        if (premiumEntity != null && premiumEntity.getMembershipId() != null) {
                            LocalDate localDateToCheck = premiumEntity.getCreationDate()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();

                            if (Objects.equals(premiumEntity.getMembershipId(), entity.getTransactionId())) {
                                if (localDateToCheck.isBefore(threeMonthsAgo)) {
                                    MembershipEditDto editDto = new MembershipEditDto();
                                    editDto.setStatus(Status.INACTIVE);
                                    editDto.setStatusReason(StatusReason.LAPSED);
                                    edit(entity.getTransactionId(), editDto);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing transactions: " + e.getMessage());
        }
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

    private static Date addDaysToDate(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private int getWaitingPeriod(String productId ,String code) {
        try {
            List<ProductAttributeDto> productAttributes = productService.getAttributes(productId);

            return productAttributes.stream()
                    .filter(attr -> attr != null && attr.getAttribute() != null)
                    .filter(attr -> {
                        FieldOptionDto attribute = attr.getAttribute();
                        return attribute != null &&
                                attribute.getCode() != null &&
                                attribute.getCode().equalsIgnoreCase(code);
                    })
                    .findFirst()
                    .map(attr -> {
                        try {
                            return Integer.parseInt(attr.getValue());
                        } catch (NumberFormatException e) {
                            System.out.println("Failed to parse waiting period value: " + attr.getValue());
                            return 0;
                        }
                    })
                    .orElse(0);
        } catch (Exception e) {
            log.info("Error retrieving waiting period: {}", e.getMessage());
            return 0;
        }
    }

    private void addEffectiveDate(TransactionDto transactionDto, MembershipCreateDto membershipCreateDto) throws Exception {
        int waitingPeriod = getWaitingPeriod(membershipCreateDto.getProductId(), "WAITING-PERIOD");

        Date today = new Date();

        TransactionDateDto dateEffective = new TransactionDateDto();
        dateEffective.setTransaction(transactionDto.getId());
        dateEffective.setType(DateType.EFFECTIVE);

        Date effectiveDate = null;

        if (membershipCreateDto.getCreationType().equalsIgnoreCase("NEW")) {
            effectiveDate = addDaysToDate(membershipCreateDto.getDateJoined(), waitingPeriod);
        }
        else if (membershipCreateDto.getCreationType().equalsIgnoreCase("UPGRADE")) {
            int upgradeWaitingPeriod = getWaitingPeriod(membershipCreateDto.getProductId(), "UPGRADE-WAITING-PERIOD");
            effectiveDate = addDaysToDate(new Date(), upgradeWaitingPeriod);
        }
        else if (membershipCreateDto.getCreationType().equalsIgnoreCase("TRANSFER")) {
            effectiveDate = today;
        }
        dateEffective.setValue(effectiveDate);

        if(dateEffective.getValue().before(today)){
            MembershipEditDto editDto = new MembershipEditDto();
            editDto.setStatus(Status.ACTIVE);
            edit(transactionDto.getId(), editDto);

        }
        transactionService.addDate(dateEffective);
    }

    private void enforceProductStatusRules(TransactionDto transactionDto) throws Exception {
        try {
            List<TransactionItemDto> items = transactionService
                    .getItems(transactionDto.getId());

            Date today = new Date();

            for (TransactionItemDto item : items) {
                String status = item.getStatus();

                if (Status.WAITING_PERIOD.equalsIgnoreCase(status) ||
                        Status.UPGRADE_WAITING_PERIOD.equalsIgnoreCase(status)) {

                    if (item.getValidTo() != null && item.getValidTo().before(today)) {
                        TransactionItemEditDto promoteDto = new TransactionItemEditDto();
                        promoteDto.setTransaction(transactionDto.getId());
                        promoteDto.setItem(item.getItem());
                        promoteDto.setProduct(item.getProduct());
                        promoteDto.setStatus(Status.ACTIVE);
                        promoteDto.setValidTo(null);

                        transactionService.editItem(promoteDto);
                    }
                }
            }

            items = transactionService.getItems(transactionDto.getId());

            TransactionItemDto latestItem = items.stream()
                    .filter(item -> item.getStatus() == null ||
                            !item.getStatus().equalsIgnoreCase(Status.INACTIVE))
                    .max(Comparator.comparing(TransactionItemDto::getValidFrom))
                    .orElse(null);

            List<TransactionItemDto> activeItems = items.stream()
                    .filter(item -> Status.ACTIVE.equalsIgnoreCase(item.getStatus()))
                    .collect(Collectors.toList());

            for (TransactionItemDto item : activeItems) {
                if (latestItem != null && item.getItem().equals(latestItem.getItem())) {
                    continue;
                }

                TransactionItemEditDto deactivateDto = new TransactionItemEditDto();
                deactivateDto.setTransaction(transactionDto.getId());
                deactivateDto.setItem(item.getItem());
                deactivateDto.setProduct(item.getProduct());
                deactivateDto.setStatus(Status.INACTIVE);
                deactivateDto.setValidTo(today);

                transactionService.editItem(deactivateDto);
            }

            if (latestItem != null) {
                MembershipEditDto membershipEditDto = new MembershipEditDto();
                membershipEditDto.setStatus(latestItem.getStatus());
                membershipEditDto.setProductId(latestItem.getProduct());

                if (latestItem.getUnitPrice() != null) {
                    membershipEditDto.setPremium(latestItem.getUnitPrice());
                }

                edit(transactionDto.getId(), membershipEditDto);

                log.info("Enforced rules: Updated membership {} with status: {} and product: {}",
                        transactionDto.getId(), latestItem.getStatus(), latestItem.getProduct());
            }

        } catch (Exception e) {
            log.error("Error enforcing product status rules: " + e.getMessage(), e);
            throw e;
        }
    }

    private void updateMembershipFromLatestItem(String membershipId) {
        try {
            List<TransactionItemDto> items = transactionService.getItems(membershipId);

            TransactionItemDto latestItem = items.stream()
                    .filter(item -> item.getStatus() == null ||
                            !item.getStatus().equalsIgnoreCase(Status.INACTIVE))
                    .max(Comparator.comparing(TransactionItemDto::getValidFrom))
                    .orElse(null);

            if (latestItem != null) {
                MembershipEditDto membershipEditDto = new MembershipEditDto();
                membershipEditDto.setStatus(latestItem.getStatus());
                membershipEditDto.setProductId(latestItem.getProduct());

                if (latestItem.getUnitPrice() != null) {
                    membershipEditDto.setPremium(latestItem.getUnitPrice());
                }

                edit(membershipId, membershipEditDto);

                log.info("Updated membership {} with status: {} and product: {}",
                        membershipId, latestItem.getStatus(), latestItem.getProduct());
            } else {
                log.warn("No active item found for membership: {}", membershipId);
            }
        } catch (Exception e) {
            log.error("Error updating membership from latest item: " + e.getMessage(), e);
        }
    }

    private void enforceUpgradeWaitingPeriodRules(TransactionDto transactionDto, String productId, TransactionItemDto previousActiveItem) throws Exception {
        try {
            List<TransactionItemDto> items = transactionService.getItems(transactionDto.getId());
            Date today = new Date();

            for (TransactionItemDto item : items) {
                if (Status.UPGRADE_WAITING_PERIOD.equalsIgnoreCase(item.getStatus())) {
                    if (item.getValidTo() != null && item.getValidTo().before(today)) {

                        TransactionItemEditDto promoteDto = new TransactionItemEditDto();
                        promoteDto.setTransaction(transactionDto.getId());
                        promoteDto.setItem(item.getItem());
                        promoteDto.setProduct(item.getProduct());
                        promoteDto.setStatus(Status.ACTIVE);
                        promoteDto.setValidTo(null);
                        transactionService.editItem(promoteDto);

                        if (previousActiveItem != null && Status.ACTIVE.equalsIgnoreCase(previousActiveItem.getStatus())) {
                            TransactionItemEditDto deactivateDto = new TransactionItemEditDto();
                            deactivateDto.setTransaction(transactionDto.getId());
                            deactivateDto.setItem(previousActiveItem.getItem());
                            deactivateDto.setProduct(previousActiveItem.getProduct());
                            deactivateDto.setStatus(Status.INACTIVE);
                            deactivateDto.setValidTo(today);
                            transactionService.editItem(deactivateDto);
                            log.info("Inactivated previous item after upgrade waiting period ended");
                        }

                        log.info("Promoted upgrade item to active after waiting period ended");
                    }
                }
            }

            items = transactionService.getItems(transactionDto.getId());
            TransactionItemDto currentlyActiveItem = items.stream()
                    .filter(item -> Status.ACTIVE.equalsIgnoreCase(item.getStatus()))
                    .max(Comparator.comparing(TransactionItemDto::getValidFrom))
                    .orElse(previousActiveItem);

            if (currentlyActiveItem != null) {
                MembershipEditDto membershipEditDto = new MembershipEditDto();
                membershipEditDto.setStatus(currentlyActiveItem.getStatus());
                membershipEditDto.setProductId(currentlyActiveItem.getProduct());
                if (currentlyActiveItem.getUnitPrice() != null) {
                    membershipEditDto.setPremium(currentlyActiveItem.getUnitPrice());
                }
                edit(transactionDto.getId(), membershipEditDto);

                log.info("Updated membership during upgrade waiting period enforcement - Status: {}, Product: {}",
                        currentlyActiveItem.getStatus(), currentlyActiveItem.getProduct());
            }

        } catch (Exception e) {
            log.error("Error enforcing upgrade waiting period rules: " + e.getMessage(), e);
            throw e;
        }
    }

    private void updateMembershipFromSpecificItem(String membershipId, TransactionItemDto item) {
        try {
            if (item != null) {
                MembershipEditDto membershipEditDto = new MembershipEditDto();
                membershipEditDto.setStatus(item.getStatus());
                membershipEditDto.setProductId(item.getProduct());

                if (item.getUnitPrice() != null) {
                    membershipEditDto.setPremium(item.getUnitPrice());
                }

                edit(membershipId, membershipEditDto);

                log.info("Updated membership {} with status: {} and product: {}",
                        membershipId, item.getStatus(), item.getProduct());
            }
        } catch (Exception e) {
            log.error("Error updating membership from specific item: " + e.getMessage(), e);
        }
    }

}
