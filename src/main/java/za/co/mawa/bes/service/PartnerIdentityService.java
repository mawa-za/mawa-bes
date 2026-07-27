package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.IdentityQueryDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityCreateDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityEditDto;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.PartnerIdentityPKEntity;
import za.co.mawa.bes.exception.DuplicateCreationException;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.Field;

import java.util.*;

@Service
public class PartnerIdentityService {
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    FieldOptionService fieldOptionService;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public void add(PartnerIdentityCreateDto partnerIdentityCreateDto) throws DuplicateCreationException {
        String type = PartnerIdentityServiceV2.normalizeIdentityType(partnerIdentityCreateDto.getType());
        String number = PartnerIdentityServiceV2.normalizeIdentityNumber(partnerIdentityCreateDto.getNumber());
        Optional<PartnerIdentityEntity> existing = partnerIdentityRepository.findByNormalizedIdentity(type, number);
        if (existing.isPresent()) {
            if (Objects.equals(existing.get().getPartner(), partnerIdentityCreateDto.getPartner())) {
                return;
            }
            throw new DuplicateCreationException(
                    "An identity record already exists for " + type + " and " + number + ".");
        }

        PartnerIdentityPKEntity partnerIdentityPK = new PartnerIdentityPKEntity();
        partnerIdentityPK.setValue(number);
        partnerIdentityPK.setType(type);
        PartnerIdentityEntity partnerIdentity = new PartnerIdentityEntity();
        partnerIdentity.setPartner(partnerIdentityCreateDto.getPartner());
        partnerIdentity.setValidFrom(partnerIdentityCreateDto.getValidFrom() != null
                ? partnerIdentityCreateDto.getValidFrom()
                : new Date());
        partnerIdentity.setValidTo(partnerIdentityCreateDto.getValidTo() != null
                ? partnerIdentityCreateDto.getValidTo()
                : Conversion.stringToDate(Constant.END_DATE));
        partnerIdentity.setPartnerIdentityPK(partnerIdentityPK);

        try {
            entityManager.persist(partnerIdentity);
            entityManager.flush();
        } catch (PersistenceException ex) {
            throw new DuplicateCreationException(
                    "An identity record already exists for " + type + " and " + number + ".");
        }
    }

    public PartnerIdentityDto get(String partner) {
        try {
            List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByPartner(partner);
            Iterator it = identityList.iterator();
            PartnerIdentityEntity partnerIdentityEntity = (PartnerIdentityEntity) it.next();
            PartnerIdentityDto partnerIdentityDto = new PartnerIdentityDto();
            partnerIdentityDto.setPartner(partnerIdentityEntity.getPartner());
            partnerIdentityDto.setNumber(partnerIdentityEntity.getPartnerIdentityPK().getValue());
            partnerIdentityDto.setType(fieldOptionService.getFieldOption(Field.ID_TYPE, partnerIdentityEntity.getPartnerIdentityPK().getType()));
            return partnerIdentityDto;
        } catch (Exception exception) {
            return null;
        }
    }

    public PartnerIdentityDto queryIdentity() {
        PartnerIdentityDto identityDto = new PartnerIdentityDto();

        return identityDto;
    }

    @Transactional
    public void edit(PartnerIdentityEditDto partnerIdentityEditDto) {
        try {
            String type = PartnerIdentityServiceV2.normalizeIdentityType(partnerIdentityEditDto.getType());
            String number = PartnerIdentityServiceV2.normalizeIdentityNumber(partnerIdentityEditDto.getNumber());
            Optional<PartnerIdentityEntity> duplicate = partnerIdentityRepository.findByNormalizedIdentity(type, number);
            if (duplicate.isPresent()
                    && !Objects.equals(duplicate.get().getPartner(), partnerIdentityEditDto.getPartner())) {
                throw new IllegalArgumentException(
                        "An identity record already exists for " + type + " and " + number + ".");
            }
            List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByPartner(partnerIdentityEditDto.getPartner());
            if (identityList != null) {
                for (PartnerIdentityEntity partnerIdentity : identityList) {
                    String validFrom = Conversion.dateToString(partnerIdentity.getValidFrom());
                    if (partnerIdentity.getPartnerIdentityPK().getType().equalsIgnoreCase(type)) {
                        PartnerIdentityPKEntity partneridentityPK = new PartnerIdentityPKEntity();
                        partneridentityPK.setType(partnerIdentity.getPartnerIdentityPK().getType());
                        partneridentityPK.setValue(partnerIdentity.getPartnerIdentityPK().getValue());
                        partnerIdentityRepository.deleteById(partneridentityPK);
                        partneridentityPK.setType(type);
                        partneridentityPK.setValue(number);
                        if (partnerIdentityEditDto.getValidTo() != null) {
                            partnerIdentity.setValidTo(partnerIdentityEditDto.getValidTo());
                        } else {
                            partnerIdentity.setValidTo(partnerIdentity.getValidTo());
                        }
                        partnerIdentity.setValidFrom(Conversion.stringToDate(validFrom));
                        partnerIdentity.setPartner(partnerIdentityEditDto.getPartner());
                        partnerIdentity.setPartnerIdentityPK(partneridentityPK);
                        partnerIdentityRepository.save(partnerIdentity);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public PartnerIdentityDto getIdentity(String type, String value) {
        Optional<PartnerIdentityEntity> identity = partnerIdentityRepository.findByNormalizedIdentity(
                PartnerIdentityServiceV2.normalizeIdentityType(type),
                PartnerIdentityServiceV2.normalizeIdentityNumber(value));
        if (identity.isEmpty()) {
            return null;
        }

        PartnerIdentityEntity entity = identity.get();
        PartnerIdentityDto dto = new PartnerIdentityDto();
        dto.setType(fieldOptionService.getFieldOption(
                Field.ID_TYPE, entity.getPartnerIdentityPK().getType()));
        dto.setNumber(entity.getPartnerIdentityPK().getValue());
        dto.setPartner(entity.getPartner());
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidTo(entity.getValidTo());
        return dto;
    }

    public ArrayList<PartnerIdentityDto> getAll(String partner) {
        ArrayList<PartnerIdentityDto> partnerIdentities = new ArrayList<>();
        List<PartnerIdentityEntity> partnerIdentityEntities = partnerIdentityRepository.findByPartner(partner);
        for (PartnerIdentityEntity partnerIdentityEntity : partnerIdentityEntities) {
            PartnerIdentityDto partnerIdentityDto = new PartnerIdentityDto();
            partnerIdentityDto.setType(fieldOptionService.getFieldOption(Field.ID_TYPE, partnerIdentityEntity.getPartnerIdentityPK().getType()));
            partnerIdentityDto.setNumber(partnerIdentityEntity.getPartnerIdentityPK().getValue());
            partnerIdentityDto.setPartner(partnerIdentityEntity.getPartner());
            partnerIdentityDto.setValidFrom(partnerIdentityEntity.getValidFrom());
            partnerIdentityDto.setValidTo(partnerIdentityEntity.getValidTo());
            partnerIdentities.add(partnerIdentityDto);
        }
        return partnerIdentities;
    }

    private Specification<PartnerIdentityEntity> findByIdentity(IdentityQueryDto queryDto) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryDto.getPartner() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partner"), queryDto.getPartner()));
            }
            if (queryDto.getValue() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerIdentityPK").get("value"), queryDto.getValue()));
            }
            if (queryDto.getType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerIdentityPK").get("type"), queryDto.getType()));
            }
            return predicate;
        };
    }

    public ArrayList<PartnerIdentityDto> getByPartnerType(String partner, String type) {
        ArrayList<PartnerIdentityDto> partnerIdentities = new ArrayList<>();
        List<PartnerIdentityEntity> partnerIdentityEntities = partnerIdentityRepository.findByPartner(partner);
        for (PartnerIdentityEntity partnerIdentityEntity : partnerIdentityEntities) {
            if (partnerIdentityEntity.getPartnerIdentityPK().getType().equals(type)) {
                PartnerIdentityDto partnerIdentityDto = new PartnerIdentityDto();
                partnerIdentityDto.setType(fieldOptionService.getFieldOption(Field.ID_TYPE, partnerIdentityEntity.getPartnerIdentityPK().getType()));
                partnerIdentityDto.setNumber(partnerIdentityEntity.getPartnerIdentityPK().getValue());
                partnerIdentityDto.setPartner(partnerIdentityEntity.getPartner());
                partnerIdentityDto.setValidFrom(partnerIdentityEntity.getValidFrom());
                partnerIdentityDto.setValidTo(partnerIdentityEntity.getValidTo());
                partnerIdentities.add(partnerIdentityDto);
            }
        }
        return partnerIdentities;
    }
}
