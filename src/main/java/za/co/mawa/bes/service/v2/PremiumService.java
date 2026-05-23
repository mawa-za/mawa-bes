package za.co.mawa.bes.service.v2;

import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.PremiumInboundDto;
import za.co.mawa.bes.dto.PrintJobRequest;
import za.co.mawa.bes.dto.premium.PremiumDto;
import za.co.mawa.bes.dto.premium.PremiumSearchDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.entity.PrintJobEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.ReceiptNumberExist;
import za.co.mawa.bes.repository.PremiumRepository;
import za.co.mawa.bes.repository.PrintJobRepository;
import za.co.mawa.bes.repository.TransactionLinkRepository;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.service.MembershipService;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.NumberRangeType;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service(value = "PremiumServiceV2")
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
    TransactionService transactionService;
    @Autowired
    PrintJobRepository printJobRepository;
    @Autowired
    PartnerService partnerService;
    @Autowired
    CompanyInfoService companyInfoService;
    @Autowired
    MembershipRepository membershipRepository;
    @Autowired
    MembershipPremiumRepository membershipPremiumRepository;
    @Autowired
    NumberAllocationService numberAllocationService;

    public PremiumEntity create(PremiumInboundDto premiumInboundDto) throws Exception {
        try {
            PremiumEntity entity = new PremiumEntity();
            entity.setId(premiumInboundDto.getDeviceReceiptId());
            if (!StringUtils.isBlank(premiumInboundDto.getReceiptNo())) {
                entity.setReceiptNumber(premiumInboundDto.getReceiptNo());
            } else {
                entity.setReceiptNumber(numberAllocationService.allocateNumber(NumberRangeType.RECEIPT));
            }
            entity.setMembershipId(premiumInboundDto.getMemberId());
            entity.setMembershipPeriod(premiumInboundDto.getPaymentPeriod());
//            entity.setLocation(premiumInboundDto.getLocation());
            entity.setTerminalId(premiumInboundDto.getDeviceId());

            String ts = premiumInboundDto.getCreatedAt();

            Instant instant = Instant.parse(ts);
            Date date = Date.from(instant);

            entity.setCreationDate(date);
            entity.setCreationTime(date);
            entity.setCreatedBy(premiumInboundDto.getUserId());
            entity.setTenderType(premiumInboundDto.getPaymentMethod().toUpperCase());
            BigDecimal amount = new BigDecimal(premiumInboundDto.getAmountCents()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            entity.setAmount(amount);
            PremiumEntity premiumEntity = premiumRepository.save(entity);
            updatePaidUpToPeriod(premiumInboundDto.getMemberId());
            return premiumEntity;
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

//    public void update(PremiumInboundDto premiumInboundDto) {
//        PremiumEntity entity = premiumRepository.getById(premiumInboundDto.getId());
//        entity.setMembershipPeriod(premiumInboundDto.getMembershipPeriod());
//        premiumRepository.save(entity);
//    }

//    public void delete(PremiumInboundDto premiumInboundDto) {
//        PremiumEntity entity = premiumRepository.getById(premiumInboundDto.getId());
//        entity.setMembershipId(null);
//        entity.setExtReceiptNumber(null);
//        premiumRepository.save(entity);
//    }

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

    public PremiumDto entityToDto(PremiumEntity entity) {
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
            premiumDto.setAmount(entity.getAmount());
//            premiumDto.setTenderType(fieldOptionService.getFieldOption(Field.TENDER_TYPE, entity.getTenderType()));
//            premiumDto.setLocation(fieldOptionService.getFieldOption(Field.SALES_AREA, entity.getLocation()));
            premiumDto.setCreationDate(formatterDate.format(entity.getCreationDate()));
            premiumDto.setCreationTime(formatterTime.format(entity.getCreationTime()));

            premiumDto.setMembershipNumber(entity.getMembershipId());

            return premiumDto;
        } else {
            return null;
        }
    }

    public ArrayList<PremiumDto> getAll() throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        //Sort sort = Sort.by("number").descending();
        //List<PremiumEntity> premiumEntities = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        List<PremiumEntity> premiumEntities = premiumRepository.findAll();
        for (PremiumEntity premiumEntity : premiumEntities) {
            try {
                premiumDtoArrayList.add(get(premiumEntity.getId()));

            } catch (Exception e) {
            }
        }
        return premiumDtoArrayList;
    }

    public ArrayList<PremiumDto> findByString(String query) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        //Sort sort = Sort.by("number").descending();
        //List<PremiumEntity> premiumEntities = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        List<PremiumEntity> premiumEntities = premiumRepository.findByString(query);
        for (PremiumEntity premiumEntity : premiumEntities) {
            try {
                premiumDtoArrayList.add(entityToDto(premiumEntity));
            } catch (Exception e) {
            }
        }
        return premiumDtoArrayList;
    }

    public ArrayList<PremiumDto> findByMembership(String id) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        List<PremiumEntity> premiumEntities = premiumRepository.findByMembership(id);
        for (PremiumEntity premiumEntity : premiumEntities) {
            try {
                premiumDtoArrayList.add(entityToDto(premiumEntity));
            } catch (Exception e) {
            }
        }
        return premiumDtoArrayList;
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

    public String determinePeriod(String id) {
        try {
            String previousPeriod = "";
            List<PremiumDto> premiums = findByMembership(id);
            PremiumDto maxItem = premiums.stream()
                    .max(Comparator.comparingInt(i -> Integer.parseInt(i.getMembershipPeriod())))
                    .orElse(null);
            if (maxItem != null) {
                previousPeriod = maxItem.getMembershipPeriod();
            } else {
                throw new Exception();
            }
            String yearString = previousPeriod.substring(0, 4);
            String monthString = previousPeriod.substring(4, 6);
            if (Integer.parseInt(monthString) == 12) {
                yearString = (Integer.toString(Integer.parseInt(yearString) + 1));
                monthString = "01";
            } else {
                int month = Integer.parseInt(monthString) + 1;
                monthString = String.format("%02d", month);
            }
            return yearString + monthString;
        } catch (Exception exception) {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            String monthString = String.format("%02d", month);
            return Integer.toString(year) + monthString;
        }
    }

    public void updatePaidUpToPeriod(String membershipId) {
        MembershipEntity membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));

        membership.setPaidUpToPeriod(getPaidUpToPeriod(membershipId));
        membershipRepository.save(membership);
    }

    public String recalculatePaidUpToPeriod(String membershipId) {
        MembershipEntity membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found: " + membershipId));

        List<MembershipPremiumEntity> paidPremiums =
                membershipPremiumRepository.findByMembershipIdAndStatusOrderByPeriodYYYYMMAsc(
                        membershipId,
                        PremiumStatus.PAID
                );

        String paidUpToPeriod = calculateContinuousPaidUpToPeriod(paidPremiums);

        membership.setPaidUpToPeriod(paidUpToPeriod);
        membership.setUpdatedAt(LocalDateTime.now());

        membershipRepository.save(membership);

        return paidUpToPeriod;
    }

    private String calculateContinuousPaidUpToPeriod(List<MembershipPremiumEntity> paidPremiums) {
        if (paidPremiums == null || paidPremiums.isEmpty()) {
            return null;
        }

        String paidUpToPeriod = null;

        for (MembershipPremiumEntity premium : paidPremiums) {
            String currentPeriod = premium.getPeriodYYYYMM();

            if (!PeriodUtil.isValidPeriod(currentPeriod)) {
                continue;
            }

            if (paidUpToPeriod == null) {
                paidUpToPeriod = currentPeriod;
                continue;
            }

            if (PeriodUtil.isNextPeriod(paidUpToPeriod, currentPeriod)) {
                paidUpToPeriod = currentPeriod;
            } else {
                break;
            }
        }

        return paidUpToPeriod;
    }

    public String getPaidUpToPeriod(String id) {
        try {
            String previousPeriod = "";
            List<PremiumDto> premiums = findByMembership(id);
            PremiumDto maxItem = premiums.stream()
                    .max(Comparator.comparingInt(i -> Integer.parseInt(i.getMembershipPeriod())))
                    .orElse(null);
            if (maxItem != null) {
                return maxItem.getMembershipPeriod();
            } else {
                throw new Exception();
            }
        } catch (Exception exception) {
            return "";
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

    public void print(PrintJobRequest printJobRequest) {
        PrintJobEntity printJobEntity = new PrintJobEntity();
        printJobEntity.setPrinterId(printJobRequest.getPrinterId());
        printJobEntity.setContent(generateReceipt(printJobRequest.getObjectId()));
        printJobRepository.save(printJobEntity);

    }

    public String generateReceipt(String id) {
        PremiumDto premiumDto = null;
        try {
            premiumDto = get(id);
        } catch (DoesNotExist e) {
            throw new RuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        String line = "------------------------------------------";
        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        sb.append(centerText(companyInfoService.getCompanyName(), 42)).append("\n");
        sb.append(centerText(companyInfoService.getCompanyAddress(), 42)).append("\n");
        sb.append(centerText("Tel: " + companyInfoService.getCompanyTelephoneNumber(), 42)).append("\n");
        sb.append(centerText("VAT No: " + companyInfoService.getVATNumber(), 42)).append("\n");
        sb.append(line).append("\n");
        sb.append("Trace ID: ").append(premiumDto.getId()).append("\n");
        sb.append("Receipt No: ").append(premiumDto.getReceiptNumber()).append("\n");
        sb.append("Received By: ").append(partnerService.getFullName(premiumDto.getEmployeeResponsible())).append("\n");
        sb.append("Print Date: ").append(dateTime).append("\n");
        sb.append(line).append("\n");
        String idnumber = "";
        if (!premiumDto.getMembership().getMember().getIdentity().equals(null)) {
            idnumber = premiumDto.getMembership().getMember().getIdentity().getNumber();
        }
        sb.append(String.format("%-20s %-20s\n", "Member ID Number:", idnumber));
        sb.append(String.format("%-20s %-20s\n", "Member Name:", partnerService.getFullName(premiumDto.getMembership().getMember())));
        sb.append(String.format("%-20s %-20s\n", "Membership Number:", premiumDto.getMembership().getNumber()));
        sb.append(String.format("%-20s %-20s\n", "Membership Option:", premiumDto.getMembership().getProduct().getDescription()));

        sb.append(line).append("\n");

        sb.append(String.format("%-20s %-20s\n", "Tender Type:", premiumDto.getTenderType().getDescription()));
        sb.append(String.format("%-20s %-20s\n", "Amount Paid:", premiumDto.getAmount()));
        String month = fieldOptionService.getOptionalFieldDescription("MONTH", premiumDto.getMembershipPeriod().substring(4, 6));
        sb.append(String.format("%-20s %-20s\n", "Payment Period:", month + " " + premiumDto.getMembershipPeriod().substring(0, 4)));
        sb.append(String.format("%-20s %-20s\n", "Payment Date:", premiumDto.getCreationDate() + " " + premiumDto.getCreationTime()));

        sb.append(line).append("\n");

        sb.append(centerText("Thank you for your support!", 42)).append("\n");

        return sb.toString();
    }

    public String centerText(String text, int width) {
        int padSize = (width - text.length()) / 2;
        String pad = " ".repeat(Math.max(0, padSize));
        return pad + text;
    }
}
