package za.co.mawa.bes.service;

import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ReceiptDao;
import za.co.mawa.bes.dto.PrintJobRequest;
import za.co.mawa.bes.dto.premium.PremiumCreateDto;
import za.co.mawa.bes.dto.premium.PremiumDto;
import za.co.mawa.bes.dto.premium.PremiumSearchDto;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.entity.PrintJobEntity;
import za.co.mawa.bes.entity.ReceiptEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributePKEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.ReceiptNumberExist;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class PremiumService {

    @Autowired
    PremiumRepository premiumRepository;
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    TransactionLinkRepository transactionLinkRepository;
    @Autowired
    TransactionAttributeService transactionAttributeService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    UserService userService;
    @Autowired
    MembershipService membershipService;
    @Autowired
    PrintJobRepository printJobRepository;

    public PremiumDto create(PremiumCreateDto premiumCreateDto) throws Exception {
        try {
            PremiumEntity entity = new PremiumEntity();
            entity.setReceiptNumber(numberRangeService.generateNumber(NumberRangeType.RECEIPT));
            try {
                if (!StringUtils.isBlank(premiumCreateDto.getExternalReceiptNo())) {
                    entity.setExtReceiptNumber(premiumCreateDto.getExternalReceiptNo());
                } else {
                    entity.setExtReceiptNumber(null);
                }
            } catch (Exception e) {
            }
            entity.setMembershipId(premiumCreateDto.getMembershipId());
            TransactionAttributeDto transactionAttributeDto = determinePeriod(premiumCreateDto.getMembershipId());
            entity.setMembershipPeriod(transactionAttributeDto.getValue());
            entity.setLocation(premiumCreateDto.getLocation());
            entity.setTerminalId(premiumCreateDto.getTerminalId());
            entity.setCreationDate(new Date());
            entity.setCreationTime(new Date());
            entity.setCreationTime(new Date());
            entity.setCreatedBy(getUser());
            entity.setTenderType(premiumCreateDto.getTenderType().toUpperCase());
            entity.setAmount(new BigDecimal(premiumCreateDto.getAmount()));
            PremiumEntity premiumEntity = premiumRepository.save(entity);
            updatePeriod(transactionAttributeDto);
            PremiumDto premiumDto = new PremiumDto();
            premiumDto.setId(premiumEntity.getId());
            return premiumDto;
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                ConstraintViolationException cve = (ConstraintViolationException) e.getCause();
                if (cve.getErrorCode() == 1062) {
                    throw new ReceiptNumberExist("Error: Receipt number already exists.");
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new Exception();
        }
    }

    public PremiumDto get(String id) throws DoesNotExist {
        PremiumEntity entity = premiumRepository.getById(id);
        if (entity != null) {
            PremiumDto premiumDto = new PremiumDto();
            SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
            premiumDto.setId(entity.getId());
            premiumDto.setReceiptNumber(entity.getReceiptNumber());
            try {
                premiumDto.setExternalReceiptNo(entity.getExtReceiptNumber());
            } catch (Exception e) {
            }

            premiumDto.setMembershipPeriod(entity.getMembershipPeriod());
            try {
                premiumDto.setMembership(membershipService.get(entity.getMembershipId()));
            } catch (Exception e) {
            }
            premiumDto.setAmount(entity.getAmount());
            premiumDto.setTenderType(fieldOptionService.getFieldOption(Field.TENDER_TYPE, entity.getTenderType()));
            premiumDto.setLocation(fieldOptionService.getFieldOption(Field.SALES_AREA, entity.getLocation()));
            premiumDto.setCreationDate(formatterDate.format(entity.getCreationDate()));
            premiumDto.setCreationTime(formatterTime.format(entity.getCreationTime()));
            try {
                premiumDto.setMembership(membershipService.get(entity.getMembershipId()));
                premiumDto.setEmployeeResponsible(userService.getUserByName(entity.getCreatedBy()).getPartner());
            } catch (Exception e) {

            }
            return premiumDto;
        } else {
            return null;
        }
    }

    public ArrayList<PremiumDto> getReceipts(PremiumSearchDto premiumSearchDto) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        //Sort sort = Sort.by("number").descending();
        //List<PremiumEntity> premiumEntities = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        List<PremiumEntity> premiumEntities = search(premiumSearchDto);
        for (PremiumEntity premiumEntity : premiumEntities) {
            try {
                premiumDtoArrayList.add(get(premiumEntity.getId()));
            } catch (Exception e) {
            }
        }
        return premiumDtoArrayList;
    }

    public ArrayList<PremiumDto> getReceiptsX(PremiumSearchDto premiumSearchDto) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        //Sort sort = Sort.by("number").descending();
        //List<PremiumEntity> premiumEntities = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        List<PremiumEntity> premiumEntities = search(premiumSearchDto);
        List<PremiumEntity> premiumEntitiesNotCashed = new ArrayList<>();
        for (PremiumEntity premiumEntity : premiumEntities) {
            try {
                TransactionLinkEntity linkEntity = transactionLinkRepository.getTransactionLinks(premiumEntity.getId(), TransactionType.CASHUP);
                if (linkEntity == null) {

                    premiumDtoArrayList.add(get(premiumEntity.getId()));
                }
            } catch (Exception e) {
            }
        }
        return premiumDtoArrayList;
    }

    private String getUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }

    private Specification<PremiumEntity> findByCriteria(PremiumSearchDto premiumSearchDto) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (premiumSearchDto.getEmployeeResponsible() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("createdBy"), premiumSearchDto.getEmployeeResponsible()));
            }
            if (premiumSearchDto.getMembershipId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("membershipId"), premiumSearchDto.getMembershipId()));
            }
            if (premiumSearchDto.getMembershipPeriod() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("membershipPeriod"), premiumSearchDto.getMembershipPeriod()));
            }
            if (premiumSearchDto.getTenderType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("tenderType"), premiumSearchDto.getTenderType()));
            }
            return predicate;
        };
    }

    private TransactionAttributeDto determinePeriod(String id) {
        try {
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(id);
            transactionAttributeDto.setAttribute(TransactionAttribute.LAST_PREMIUM_PERIOD);
            String previousPeriod = transactionAttributeService.get(transactionAttributeDto);
            String yearString = previousPeriod.substring(0, 4);
            String monthString = previousPeriod.substring(4, 6);
            if (Integer.parseInt(monthString) == 12) {
                yearString = (Integer.toString(Integer.parseInt(yearString) + 1));
                monthString = "01";
            } else {
                int month = Integer.parseInt(monthString) + 1;
                monthString = String.format("%02d", month);
            }
            transactionAttributeDto.setValue(yearString + monthString);
            return transactionAttributeDto;
        } catch (Exception exception) {
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(id);
            transactionAttributeDto.setAttribute(TransactionAttribute.LAST_PREMIUM_PERIOD);
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            String monthString = String.format("%02d", month);
            transactionAttributeDto.setValue(Integer.toString(year) + monthString);
            return transactionAttributeDto;
        }
    }

    private void updatePeriod(TransactionAttributeDto transactionAttributeDto) {
        try {
            transactionAttributeService.edit(transactionAttributeDto);
        } catch (Exception exception) {
            transactionAttributeService.add(transactionAttributeDto);
        }
    }


    public List<PremiumEntity> search(PremiumSearchDto premiumSearchDto) {
        List<PremiumEntity> premiumEntityList = premiumRepository.findAll();
        List<PremiumEntity> premiumEntities = new ArrayList<>();

        for (PremiumEntity premium : premiumEntityList) {
            try {

                boolean match = true;

                if (premiumSearchDto.getEmployeeResponsible() != null) {

                    match = premium.getCreatedBy().equals(premiumSearchDto.getEmployeeResponsible());
                }

                if (premiumSearchDto.getTenderType() != null) {

                    match = match && premium.getTenderType().equals(premiumSearchDto.getTenderType());
                }

                if (premiumSearchDto.getMembershipId() != null) {
                    match = match && premium.getMembershipId().equals(premiumSearchDto.getMembershipId());
                }

                if (premiumSearchDto.getLocation() != null) {

                    match = match && premium.getLocation().equals(premiumSearchDto.getLocation());
                }

                if (match) {
                    premiumEntities.add(premium);
                }

            } catch (Exception e) {

            }
        }
        return premiumEntities;
    }
    public void print(PrintJobRequest printJobRequest){
        try {
            PremiumDto premiumDto = get(printJobRequest.getObjectId());
            PrintJobEntity printJobEntity = new PrintJobEntity();
            printJobEntity.setPrinterId(printJobRequest.getPrinterId());
            printJobEntity.setContent(generateReceipt());
            printJobRepository.save(printJobEntity);

        } catch (DoesNotExist e) {
            throw new RuntimeException(e);
        }
    }

    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        String line = "------------------------------------------";
        String storeName = "MY STORE";
        String cashier = "John";
        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        sb.append(centerText(storeName, 42)).append("\n");
        sb.append(centerText("VAT No: 123456789", 42)).append("\n");
        sb.append(line).append("\n");
        sb.append("Cashier: ").append(cashier).append("\n");
        sb.append("Date: ").append(dateTime).append("\n");
        sb.append(line).append("\n");

        sb.append(String.format("%-20s %5s %7s\n", "Item", "Qty", "Total"));
        sb.append(String.format("%-20s %5d %7.2f\n", "Coca Cola 500ml", 2, 30.00));
        sb.append(String.format("%-20s %5d %7.2f\n", "Chips Large", 1, 20.00));
        sb.append(String.format("%-20s %5d %7.2f\n", "Sandwich", 1, 25.00));

        sb.append(line).append("\n");

        double subtotal = 75.00;
        double tax = 11.25;
        double total = 86.25;
        double cash = 100.00;
        double change = cash - total;

        sb.append(String.format("%-20s %20.2f\n", "Subtotal:", subtotal));
        sb.append(String.format("%-20s %20.2f\n", "Tax (15%):", tax));
        sb.append(String.format("%-20s %20.2f\n", "Total:", total));
        sb.append(String.format("%-20s %20.2f\n", "Cash:", cash));
        sb.append(String.format("%-20s %20.2f\n", "Change:", change));
        sb.append(line).append("\n");

        sb.append(centerText("Thank you for shopping!", 42)).append("\n");
        sb.append(centerText("Visit again!", 42)).append("\n");

        return sb.toString();
    }

    public String centerText(String text, int width) {
        int padSize = (width - text.length()) / 2;
        String pad = " ".repeat(Math.max(0, padSize));
        return pad + text;
    }
}
