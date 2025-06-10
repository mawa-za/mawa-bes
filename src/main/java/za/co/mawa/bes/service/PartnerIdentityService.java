package za.co.mawa.bes.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.IdentityDto;
import za.co.mawa.bes.dto.IdentityQueryDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
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

    public void add(PartnerIdentityCreateDto partnerIdentityCreateDto) throws DuplicateCreationException {
        try {
//            for (PartnerIdentityEntity identityEntity : partnerIdentityRepository.findPartnerIdentityByPartner(partnerIdentityCreateDto.getPartner())) {
//                if (identityEntity.getPartnerIdentityPK().getType().equals(partnerIdentityCreateDto.getType())) {
//                    throw new RuntimeException("Duplicate identity type found for partner");
//                }
//            }
            Sort sort = Sort.by("partnerIdentityPK").descending();
            IdentityQueryDto query = new IdentityQueryDto();
            query.setValue(partnerIdentityCreateDto.getNumber());
            query.setType(partnerIdentityCreateDto.getType());
            List<PartnerIdentityEntity> identityEntities = partnerIdentityRepository.findAll(findByIdentity(query), sort);
            if (identityEntities.size() > 0) {
                throw new DuplicateCreationException("Duplicate identity type found");
            }
            PartnerIdentityPKEntity partnerIdentityPK = new PartnerIdentityPKEntity();
            partnerIdentityPK.setValue(partnerIdentityCreateDto.getNumber());
            partnerIdentityPK.setType(partnerIdentityCreateDto.getType());
            PartnerIdentityEntity partnerIdentity = new PartnerIdentityEntity();
            partnerIdentity.setPartner(partnerIdentityCreateDto.getPartner());
            if (partnerIdentityCreateDto.getValidFrom() != null) {
                partnerIdentity.setValidFrom(partnerIdentityCreateDto.getValidFrom());
            } else {
                partnerIdentity.setValidFrom(new Date());
            }
            if (partnerIdentityCreateDto.getValidTo() != null) {
                partnerIdentity.setValidTo(partnerIdentityCreateDto.getValidTo());
            } else {
                partnerIdentity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            }
            partnerIdentity.setPartnerIdentityPK(partnerIdentityPK);
            partnerIdentityRepository.save(partnerIdentity);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public void edit(PartnerIdentityEditDto partnerIdentityEditDto) {
        try {
            List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByPartner(partnerIdentityEditDto.getPartner());
            if (identityList != null) {
                for (PartnerIdentityEntity partnerIdentity : identityList) {
                    String validFrom = Conversion.dateToString(partnerIdentity.getValidFrom());
                    if (partnerIdentity.getPartnerIdentityPK().getType().equals(partnerIdentityEditDto.getType())) {
                        PartnerIdentityPKEntity partneridentityPK = new PartnerIdentityPKEntity();
                        partneridentityPK.setType(partnerIdentityEditDto.getType());
                        partneridentityPK.setValue(partnerIdentity.getPartnerIdentityPK().getValue());
                        partnerIdentityRepository.deleteById(partneridentityPK);
                        partneridentityPK.setType(partnerIdentityEditDto.getType());
                        partneridentityPK.setValue(partnerIdentityEditDto.getNumber());
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

        PartnerIdentityPKEntity pk = new PartnerIdentityPKEntity();
        pk.setValue(value);
        pk.setType(type);
        Optional<PartnerIdentityEntity> id = partnerIdentityRepository.findById(pk);
        if (!id.isEmpty()) {
            PartnerIdentityDto partnerIdentityDto = new PartnerIdentityDto();
            partnerIdentityDto.setType(fieldOptionService.getFieldOption(Field.ID_TYPE, id.get().getPartnerIdentityPK().getType()));
            partnerIdentityDto.setNumber(id.get().getPartnerIdentityPK().getValue());
            partnerIdentityDto.setPartner(id.get().getPartner());
            partnerIdentityDto.setValidFrom(id.get().getValidFrom());
            partnerIdentityDto.setValidTo(id.get().getValidTo());
            return partnerIdentityDto;
        }else{
            return null;
        }

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
