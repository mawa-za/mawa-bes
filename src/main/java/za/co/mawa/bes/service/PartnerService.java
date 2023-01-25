package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.ContactDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.RelationDto;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.dao.PartnerDao;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.StringConversion;
import za.co.raretag.mawabes.dto.PartnerQueryDto;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Service
public class PartnerService implements PartnerDao {

    @Autowired
    PartnerRoleRepository partnerRoleRepository;
    @Autowired
    PartnerRelationRepository partnerRelationRepository;
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    PartnerRepository partnerRepository;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    PartnerContactRepository partnerContactRepository;
    @Override
    public String create(PartnerEntity partnerEntity) {
        return null;
    }

    @Override
    public String create(PartnerDto object) {
        String partnerId = null;
        try {
            PartnerEntity entity = new PartnerEntity();
            partnerId = numberRangeService.generateNumber(object.getType());
            entity.setId(partnerId);
            entity.setType(object.getType());
            if (object.getName1() != null) {
                entity.setName1(object.getName1().toUpperCase());
            }
            if (object.getName2() != null) {
                entity.setName2(object.getName2().toUpperCase());
            }
            if (object.getName3() != null) {
                entity.setName3(object.getName3().toUpperCase());
            }
            entity.setBirthDate(Conversion.stringToDate(object.getBirthDate()));
            entity.setGender(object.getGender());
            entity.setLanguage(object.getLanguage());
            entity.setMaritalStatus(object.getMaritalStatus());
            entity.setTitle(object.getTitle());
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            partnerRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return partnerId;
    }

    @Override
    public boolean edit(PersonDto object) {

        try {
            PartnerEntity partner = partnerRepository.getById(object.getId());

            if (partner != null) {
                if (object.getLastName() != null) {
                    partner.setName1(object.getLastName().toUpperCase());
                }
                if (object.getFirstName() != null) {
                    partner.setName2(object.getFirstName().toUpperCase());
                }

                if (object.getMiddleName() != null) {
                    partner.setName3(object.getMiddleName().toUpperCase());
                }
                if (object.getBirthDate() != null) {
                    partner.setBirthDate(Conversion.stringToDate(object.getBirthDate()));
                }
                if (object.getGender() != null) {
                    partner.setGender(object.getGender());
                }
                if (object.getLanguage() != null) {
                    partner.setLanguage(object.getLanguage());
                }
                if (object.getMaritalStatus() != null) {
                    partner.setMaritalStatus(object.getMaritalStatus());
                }
                if (object.getTitle() != null) {
                    partner.setTitle(object.getTitle());
                }
                if (object.getStatus() != null) {
                    partner.setStatus(object.getStatus());
                }

                partnerRepository.save(partner);

                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public PartnerEntity findById(String id) {
        return null;
    }
    @Override
    public PartnerDto get(String id) {
        PartnerDto object = null;
        PartnerEntity partner = partnerRepository.getById(id);
        if (partner != null) {
            object = entityToObject(partner);
        }
        return object;
    }

    private PartnerDto entityToObject(PartnerEntity partner) {
        PartnerDto object = new PartnerDto();
        object.setId(partner.getId());
        object.setName1(partner.getName1());
        object.setName2(partner.getName2());
        object.setName3(partner.getName3());
        object.setBirthDate(Conversion.dateToString(partner.getBirthDate()));
        object.setStatus(StringConversion.capitalizeFully(partner.getStatus()));
        object.setTitle(fieldOptionService.getFieldOptionDescription("TITLE", partner.getTitle()));
        object.setGender(fieldOptionService.getFieldOptionDescription("GENDER", partner.getGender()));
        object.setMaritalStatus(fieldOptionService.getFieldOptionDescription("MARITALSTATUS", partner.getMaritalStatus()));
        object.setLanguage(fieldOptionService.getFieldOptionDescription("LANGUAGE", partner.getLanguage()));
        object.setValidFrom(Conversion.dateToString(partner.getValidFrom()));
        object.setValidTo(Conversion.dateToString(partner.getValidTo()));

        PartnerIdentityEntity partnerIdentity = getPartnerIdentityNo(partner.getId());
        if (partnerIdentity != null) {
            object.setIdType(fieldOptionService.getFieldOptionDescription("IDTYPE", partnerIdentity.getPartnerIdentityPK().getType()));
            object.setIdNumber(partnerIdentity.getPartnerIdentityPK().getValue());
        }

        return object;
    }

    private PartnerIdentityEntity getPartnerIdentityNo(String partner) {
        PartnerIdentityEntity partnerIdentity = null;
        List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByPartner(partner);
        Iterator it = identityList.iterator();
        if (it.hasNext()) {
            partnerIdentity = (PartnerIdentityEntity) it.next();

        }
        return partnerIdentity;
    }

    private PersonDto partnerToPerson(PartnerEntity partner) {
        PersonDto person = new PersonDto();
        person.setId(partner.getId());
        person.setLastName(partner.getName1());
        person.setFirstName(partner.getName2());
        person.setMiddleName(partner.getName3());
        person.setFullName(partner.getName1() + " " + partner.getName2());
        person.setType(partner.getType());
        person.setTitle(fieldOptionService.getFieldOptionDescription("TITLE", partner.getTitle()));
        person.setStatus(fieldOptionService.getFieldOptionDescription("PARTNERSTATUS", partner.getStatus()));
        person.setBirthDate(Conversion.dateToString(partner.getBirthDate()));
        person.setGender(fieldOptionService.getFieldOptionDescription("GENDER", partner.getGender()));
        person.setMaritalStatus(fieldOptionService.getFieldOptionDescription("MARITALSTATUS", partner.getMaritalStatus()));
        return person;
    }
    private ContactDto getContact(ContactDto contact) {
        try {
            PartnerContactPKEntity partnerContactPK = new PartnerContactPKEntity();
            partnerContactPK.setPartner(contact.getPartner());
            partnerContactPK.setType(contact.getType());
            PartnerContactEntity partnerContact = partnerContactRepository.getById(partnerContactPK);
            if (partnerContact != null) {
                contact.setDetail(partnerContact.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return contact;
    }
    @Override
    public boolean removeRole(String partner, String role) {
        boolean removed = false;
        try {
            PartnerRolePKEntity partnerRolePK = new PartnerRolePKEntity();
            partnerRolePK.setId(partner);
            partnerRolePK.setRole(role);

            partnerRoleRepository.deleteById(partnerRolePK);
            removed = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return removed;
    }

    @Override
    public ArrayList<RelationDto> getRelationByPartner2(String partner2) {
        ArrayList<RelationDto> relations = new ArrayList<>();
        List<PartnerRelationEntity> relationsPartner = partnerRelationRepository.findPartnerRelationByPartner2(partner2);
        for (PartnerRelationEntity partnerRelation : relationsPartner) {
            RelationDto relation = new RelationDto();
            relation.setPartner1(partnerRelation.getPartnerRelationPK().getPartner1());
            relation.setPartner2(partnerRelation.getPartnerRelationPK().getPartner2());
            relation.setType(partnerRelation.getPartnerRelationPK().getType());
            relations.add(relation);
        }
        return relations;
    }

    @Override
    public ArrayList<PartnerDto> search(PartnerQueryDto pq) {
        ArrayList<PartnerDto> finalList = new ArrayList<>();
        ArrayList<PartnerDto> filteredList = new ArrayList<>();
        ArrayList<PartnerDto> initialList = new ArrayList<>();

        if (pq == null) {
            List<PartnerEntity> partnerList = partnerRepository.findAll();
            for (PartnerEntity partner : partnerList) {
                finalList.add(entityToObject(partner));
            }
            return finalList;
        }

        if (pq.getRole() != null) {
            List<PartnerRoleEntity> partnerRoleList = partnerRoleRepository.findPartnerByRole(pq.getRole());
            for (PartnerRoleEntity partnerRole : partnerRoleList) {
                PartnerEntity partner = partnerRepository.getById(partnerRole.getPartnerRolePK().getId());
                if (partner != null) {
                    initialList.add(entityToObject(partner));
                }
            }
        }

        if (pq.getIdNumber() != null) {
            List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByValue(pq.getIdNumber());
            for (PartnerIdentityEntity partnerIdentity : identityList) {
                PartnerEntity partner = partnerRepository.getById(partnerIdentity.getPartner());
                if (partner != null) {
                    initialList.add(entityToObject(partner));
                }
            }
        }

        if (pq.getIdType() != null && pq.getIdNumber() != null) {
            PartnerIdentityPKEntity identity = new PartnerIdentityPKEntity();
            identity.setType(pq.getIdType());
            identity.setValue(pq.getIdNumber());
            PartnerIdentityEntity partnerIdentity = partnerIdentityRepository.getById(identity);
            if (partnerIdentity != null) {
                PartnerEntity partner = partnerRepository.getById(partnerIdentity.getPartner());
                if (partner != null) {
                    initialList.add(entityToObject(partner));
                }
            }

        }

        if (pq.getCellphone() != null) {
            List<PartnerContactEntity> contactList = partnerContactRepository.findPartnerByValue(pq.getCellphone());
            for (PartnerContactEntity partnerContact : contactList) {
                PartnerEntity partner = partnerRepository.getById(partnerContact.getPartnerContactPK().getPartner());
                initialList.add(entityToObject(partner));
            }

        }

        if (pq.getEmail() != null) {
            List<PartnerContactEntity> contactList = partnerContactRepository.findPartnerByValue(pq.getEmail());
            for (PartnerContactEntity partnerContact : contactList) {
                PartnerEntity partner = partnerRepository.getById(partnerContact.getPartnerContactPK().getPartner());
                initialList.add(entityToObject(partner));
            }

        }

        if (pq.getName1() != null) {
            List<PartnerEntity> partnerList = partnerRepository.findPartnerByName1(pq.getName1());
            for (PartnerEntity partner : partnerList) {
                initialList.add(entityToObject(partner));
            }
        }
        if (pq.getName2() != null) {
            List<PartnerEntity> partnerList = partnerRepository.findPartnerByName2(pq.getName2());
            for (PartnerEntity partner : partnerList) {
                initialList.add(entityToObject(partner));
            }
        }
        if (pq.getName3() != null) {
            List<PartnerEntity> partnerList = partnerRepository.findPartnerByName3(pq.getName3());
            for (PartnerEntity partner : partnerList) {
                initialList.add(entityToObject(partner));
            }
        }

        if (pq.getFilter() != null) {
            List<PartnerEntity> partnerList = partnerRepository.findAll();
            for (PartnerEntity partner : partnerList) {
                initialList.add(entityToObject(partner));
            }
        }

        for (PartnerDto pqr : initialList) {
            if (pq.getIdType() != null && !"".equals(pq.getIdType())) {
                if (!pqr.getIdType().equals(pq.getIdType())) {
                    continue;
                }
            }

            if (pq.getIdNumber() != null && !"".equals(pq.getIdNumber())) {
                if (!pqr.getIdNumber().equals(pq.getIdNumber())) {
                    continue;
                }
            }

            if (pq.getName1() != null && !"".equals(pq.getName1())) {
                if (!pqr.getName1().equals(pq.getName1())) {
                    continue;
                }
            }

            if (pq.getName2() != null && !"".equals(pq.getName2())) {
                if (!pqr.getName2().equals(pq.getName2())) {

                    continue;
                }
            }
            if (pq.getName3() != null && !"".equals(pq.getName3())) {
                if (!pqr.getName3().equals(pq.getName3())) {
                    continue;
                }
            }
            if (pq.getRole() != null && !"".equals(pq.getRole())) {
                PartnerRolePKEntity rolePK = new PartnerRolePKEntity();
                rolePK.setId(pqr.getId());
                rolePK.setRole(pq.getRole());
                if (partnerRoleRepository.getById(rolePK) == null) {
                    continue;
                }
            }
            filteredList.add(pqr);
        }

        String searchStr = "";
        for (PartnerDto pqr : filteredList) {
            if (!searchStr.contains(pqr.getId() + '|')) {
                searchStr = searchStr + pqr.getId() + '|';
                finalList.add(pqr);
            }
        }
        return finalList;
    }
}
