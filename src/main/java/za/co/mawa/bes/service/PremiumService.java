package za.co.mawa.bes.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ReceiptDao;
import za.co.mawa.bes.dto.premium.PremiumCreateDto;
import za.co.mawa.bes.dto.premium.PremiumDto;
import za.co.mawa.bes.dto.premium.PremiumSearchDto;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.entity.ReceiptEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributePKEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.repository.PremiumRepository;
import za.co.mawa.bes.repository.ReceiptRepository;
import za.co.mawa.bes.repository.TransactionAttributeRepository;
import za.co.mawa.bes.repository.TransactionLinkRepository;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
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
    PartnerService partnerService;

    public PremiumDto create(PremiumCreateDto premiumCreateDto) throws Exception {
        try {
            PremiumEntity entity = new PremiumEntity();
            entity.setReceiptNumber(numberRangeService.generateNumber(NumberRangeType.RECEIPT));
            entity.setExtReceiptNumber(premiumCreateDto.getExternalReceiptNo());
            entity.setMembershipId(premiumCreateDto.getMembershipId());
            entity.setMembershipPeriod(determinePeriod(premiumCreateDto.getMembershipId()));
//            entity.setLocation(premiumCreateDto.getLocation());
            entity.setEmployee_responsible(premiumCreateDto.getEmployeeResponsible());
            entity.setTerminalId(premiumCreateDto.getTerminalId());
            entity.setCreationDate(new Date());
            entity.setCreationTime(new Date());
            entity.setCreationTime(new Date());
            entity.setCreatedBy(getUser());
            entity.setTenderType(premiumCreateDto.getTenderType().toUpperCase());
            entity.setAmount(new BigDecimal(premiumCreateDto.getAmount()));
            PremiumEntity premiumEntity = premiumRepository.save(entity);
            PremiumDto premiumDto = new PremiumDto();
            premiumDto.setId(premiumEntity.getId());
            return premiumDto;
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
            premiumDto.setExternalReceiptNo(entity.getExtReceiptNumber());
            premiumDto.setMembershipPeriod(entity.getMembershipPeriod());
            premiumDto.setAmount(entity.getAmount());
            premiumDto.setTenderType(fieldOptionService.getFieldOption(Field.TENDER_TYPE, entity.getTenderType()));
//            premiumDto.setLocation(fieldOptionService.getFieldOption(Field.SALES_AREA, entity.getLocation()));
            premiumDto.setCreationDate(formatterDate.format(entity.getCreationDate()));
            premiumDto.setCreationTime(formatterTime.format(entity.getCreationTime()));
            try {
                premiumDto.setMembership(membershipService.get(entity.getMembershipId()));
                premiumDto.setCreatedBy(userService.getUserByName(entity.getCreatedBy()).getPartner());
                premiumDto.setEmployeeResponsible(partnerService.get(entity.getEmployee_responsible()));
            } catch (Exception e) {

            }
            return premiumDto;
        }else{
            return null;
        }
    }

    public ArrayList<PremiumDto> getReceipts(PremiumSearchDto premiumSearchDto) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        Sort sort = Sort.by("number").descending();
        List<PremiumEntity> premiumEntities = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        for(PremiumEntity premiumEntity: premiumEntities){
            premiumDtoArrayList.add(get(premiumEntity.getId()));
        }
        return premiumDtoArrayList;
    }

    public ArrayList<PremiumDto> getReceiptsX(PremiumSearchDto premiumSearchDto) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        Sort sort = Sort.by("number").descending();
        List<PremiumEntity> premiumEntities = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        List<PremiumEntity> premiumEntitiesNotCashed = new ArrayList<>();
        for (PremiumEntity premiumEntity : premiumEntities) {
            TransactionLinkEntity linkEntity = transactionLinkRepository.getTransactionLinks(premiumEntity.getId(), TransactionType.CASHUP);
            if (linkEntity == null) {
                premiumDtoArrayList.add(get(premiumEntity.getId()));
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

    private String determinePeriod(String id) {
        try {
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(id);
            transactionAttributeDto.setAttribute(TransactionAttribute.LAST_PREMIUM_PERIOD);
            String previousPeriod = transactionAttributeService.get(transactionAttributeDto);
            String yearString = previousPeriod.substring(0,4);
            String monthString = previousPeriod.substring(4,6);
            if (Integer.parseInt(monthString) == 12){
                yearString = (Integer.toString(Integer.parseInt(yearString) + 1));
                monthString = "01";
            }else{
                int month = Integer.parseInt(monthString) + 1;
                monthString = String.format("%02d", month);
            }
            transactionAttributeDto.setValue(yearString + monthString);
            transactionAttributeService.edit(transactionAttributeDto);
            return transactionAttributeDto.getValue();
        } catch (Exception exception) {
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(id);
            transactionAttributeDto.setAttribute(TransactionAttribute.LAST_PREMIUM_PERIOD);
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            String monthString = String.format("%02d", month);
            transactionAttributeDto.setValue(Integer.toString(year) + monthString);
            transactionAttributeService.add(transactionAttributeDto);
            return transactionAttributeDto.getValue();
        }
    }

    public List<PremiumEntity> search(PremiumSearchDto premiumSearchDto) {
        List<PremiumEntity> premiumEntityList = premiumRepository.findAll();
        List<PremiumEntity> premiumEntities = new ArrayList<>();

        for (PremiumEntity premium : premiumEntityList) {
            try {

                boolean match = true;

                if(premiumSearchDto.getEmployeeResponsible() != null) {

                    match =  premium.getEmployee_responsible().equals(premiumSearchDto.getEmployeeResponsible());
                }

                if(premiumSearchDto.getCreatedBy() != null){

                    match = match && premium.getCreatedBy().equals(premiumSearchDto.getCreatedBy());
                }

                if(premiumSearchDto.getTenderType() != null) {

                    match =  match && premium.getTenderType().equals(premiumSearchDto.getTenderType());
                }

                if(premiumSearchDto.getMembershipId() != null){
                    match = match && premium.getMembershipId().equals(premiumSearchDto.getMembershipId());
                }

//                if(premiumSearchDto.getLocation() !=null){
//
//                    match = match && premium.getLocation().equals(premiumSearchDto.getLocation());
//                }

                if(match) {
                    premiumEntities.add(premium);
                }

            }catch (Exception e){

            }
        }
        return premiumEntities;
    }

}
