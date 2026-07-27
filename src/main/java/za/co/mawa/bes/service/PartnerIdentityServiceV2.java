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
import za.co.mawa.bes.dto.partner.PartnerIdentityDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityEditDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityInboundDto;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.PartnerIdentityPKEntity;
import za.co.mawa.bes.exception.DuplicateCreationException;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.Field;

import java.util.*;

@Service
public class PartnerIdentityServiceV2 {
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    FieldOptionService fieldOptionService;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public void add(PartnerIdentityInboundDto partnerIdentityInboundDto) throws DuplicateCreationException {
        String type = normalizeIdentityType(partnerIdentityInboundDto.getType());
        String number = normalizeIdentityNumber(partnerIdentityInboundDto.getNumber());
        Optional<PartnerIdentityEntity> existing = partnerIdentityRepository.findByNormalizedIdentity(type, number);
        if (existing.isPresent()) {
            if (Objects.equals(existing.get().getPartner(), partnerIdentityInboundDto.getPartner())) {
                return;
            }
            throw new DuplicateCreationException(
                    "An identity record already exists for " + type + " and " + number + ".");
        }

        PartnerIdentityPKEntity partnerIdentityPK = new PartnerIdentityPKEntity();
        partnerIdentityPK.setValue(number);
        partnerIdentityPK.setType(type);
        PartnerIdentityEntity partnerIdentity = new PartnerIdentityEntity();
        partnerIdentity.setPartner(partnerIdentityInboundDto.getPartner());
        partnerIdentity.setValidFrom(partnerIdentityInboundDto.getValidFrom() != null
                ? partnerIdentityInboundDto.getValidFrom()
                : new Date());
        partnerIdentity.setValidTo(partnerIdentityInboundDto.getValidTo() != null
                ? partnerIdentityInboundDto.getValidTo()
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
    public PartnerIdentityDto queryIdentity(){
        PartnerIdentityDto identityDto = new PartnerIdentityDto();

        return identityDto;
    }
    @Transactional
    public void edit(PartnerIdentityEditDto partnerIdentityEditDto) {
        try {
            String type = normalizeIdentityType(partnerIdentityEditDto.getType());
            String number = normalizeIdentityNumber(partnerIdentityEditDto.getNumber());
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
                normalizeIdentityType(type),
                normalizeIdentityNumber(value));
        return identity.map(this::toDto).orElse(null);
    }

    public boolean exists(String type, String value) {
        return partnerIdentityRepository.findByNormalizedIdentity(
                normalizeIdentityType(type),
                normalizeIdentityNumber(value)).isPresent();
    }

    public static String normalizeIdentityType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Identity type is required.");
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizeIdentityNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("Identity number is required.");
        }
        return number.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private PartnerIdentityDto toDto(PartnerIdentityEntity entity) {
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
}
