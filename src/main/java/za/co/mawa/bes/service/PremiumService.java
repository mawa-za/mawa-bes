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
import za.co.mawa.bes.utils.NumberRangeType;
import za.co.mawa.bes.utils.ReceiptType;
import za.co.mawa.bes.utils.TransactionAttribute;
import za.co.mawa.bes.utils.TransactionType;

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
    TransactionAttributeRepository transactionAttributeRepository;

    public PremiumDto create(PremiumCreateDto premiumCreateDto) throws Exception {
        try {
            PremiumEntity entity = new PremiumEntity();
            entity.setPremiumNumber(numberRangeService.generateNumber(NumberRangeType.PREMIUM));
            entity.setMembershipNumber(premiumCreateDto.getMembershipNumber());
            entity.setMembershipPeriod(determinePeriod(premiumCreateDto.getMembershipNumber()));
            entity.setLocation(premiumCreateDto.getLocation());
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
            SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
            PremiumDto premiumDto = new PremiumDto();
            premiumDto.setId(entity.getId());
            premiumDto.setReceiptNumber(entity.getPremiumNumber());
            premiumDto.setMembershipNumber(entity.getMembershipNumber());
            premiumDto.setMembershipPeriod(entity.getMembershipPeriod());
            premiumDto.setTenderType(entity.getTenderType());
            premiumDto.setAmount(entity.getAmount().toString());
            premiumDto.setEmployeeResponsible(entity.getCreatedBy());
            premiumDto.setCreationDate(formatterDate.format(entity.getCreationDate()));
            premiumDto.setCreationDate(formatterTime.format(entity.getCreationTime()));
            return premiumDto;
        } else {
            throw new DoesNotExist();
        }
    }


    public ArrayList<PremiumDto> getReceipts(PremiumSearchDto premiumSearchDto) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<PremiumEntity> receipts = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        return premiumDtoArrayList;
    }


    public ArrayList<PremiumDto> getReceiptsX(PremiumSearchDto premiumSearchDto) throws Exception {
        ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<PremiumEntity> premiumEntities = premiumRepository.findAll(findByCriteria(premiumSearchDto), sort);
        List<PremiumEntity> premiumEntitiesNotCashed = new ArrayList<>();
        for (PremiumEntity premiumEntity : premiumEntities) {
            TransactionLinkEntity linkEntity = transactionLinkRepository.getTransactionLinks(premiumEntity.getId(), TransactionType.CASHUP);
            if (linkEntity == null) {
                premiumEntitiesNotCashed.add(premiumEntity);
            }
        }
        return premiumDtoArrayList;
    }

    private ReceiptDto entityIDtoDto(ReceiptEntity entity) throws Exception {
        try {
            ReceiptDto receipt = new ReceiptDto();
            receipt.setId(entity.getId());
            return receipt;
        } catch (Exception e) {
            throw new Exception();
        }

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
            if (premiumSearchDto.getMembershipNumber() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("membershipNumber"), premiumSearchDto.getMembershipNumber()));
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
            List<TransactionAttributeEntity> transactionAttributeEntityList = transactionAttributeRepository.find(id,TransactionAttribute.LAST_PREMIUM_PERIOD);
            TransactionAttributeEntity transactionAttributeEntity = transactionAttributeEntityList.iterator().next();
            String previousPeriod =  transactionAttributeEntity.getValue();
            String yearString = previousPeriod.substring(0,4);
            String monthString = previousPeriod.substring(4,6);
            if (Integer.parseInt(monthString) == 12){
                yearString = (Integer.toString(Integer.parseInt(yearString) + 1));
                monthString = "01";
            }else{
                int month = Integer.parseInt(monthString) + 1;
                monthString = String.format("%02d", month);
            }
            transactionAttributeEntity.setValue(yearString + monthString);
            transactionAttributeEntity.setValidTo(new Date());
            transactionAttributeEntity.setValidFrom(new Date());
            transactionAttributeRepository.save(transactionAttributeEntity);
            return transactionAttributeEntity.getValue();
        } catch (Exception exception) {
            TransactionAttributeEntity transactionAttributeEntity = new TransactionAttributeEntity();
            transactionAttributeEntity.setTransaction(id);
            transactionAttributeEntity.setAttribute(TransactionAttribute.LAST_PREMIUM_PERIOD);
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            String monthString = String.format("%02d", month);
            transactionAttributeEntity.setValue(Integer.toString(year) + monthString);
            transactionAttributeEntity.setValidTo(new Date());
            transactionAttributeEntity.setValidFrom(new Date());
            transactionAttributeRepository.save(transactionAttributeEntity);
            return transactionAttributeEntity.getValue();
        }
    }
}
