package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.prospect.ProspectDto;
import za.co.mawa.bes.dto.prospect.ProspectEditDto;
import za.co.mawa.bes.dto.prospect.ProspectSearchDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.dao.PartnerDao;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.exception.PartnerNotFound;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.dto.PartnerQueryDto;

import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    PartnerAddressRepository partnerAddressRepository;
    @Autowired
    AddressRepository addressRepository;
    @Autowired
    PartnerBankAccountRepository partnerBankAccountRepository;
    @Autowired
    PartnerResourceApiRepository partnerResourceApiRepository;
    @Autowired
    PartnerAttachmentRepository partnerAttachmentRepository;
    @Autowired
    UserService userService;
    @Autowired
    PartnerDateRepository partnerDateRepository;
    // @Override
//    public String create(PartnerEntity partnerEntity) {
//        return null;
//    }

    @Override
    public PartnerDto create(PartnerDto partnerDto) {

        try {
            PartnerEntity entity = new PartnerEntity();
            String partnerNo = numberRangeService.generateNumber(partnerDto.getType());
            entity.setNo(partnerNo);
            entity.setType(partnerDto.getType().toUpperCase());
            if (partnerDto.getName1() != null) {
                entity.setName1(partnerDto.getName1().toUpperCase());
            }
            if (partnerDto.getName2() != null) {
                entity.setName2(partnerDto.getName2().toUpperCase());
            }
            if (partnerDto.getName3() != null) {
                entity.setName3(partnerDto.getName3().toUpperCase());
            }
            if (partnerDto.getBirthDate() != null) {
                entity.setBirthDate(Conversion.stringToDate(partnerDto.getBirthDate()));
            }
            if (partnerDto.getGender() != null) {
                entity.setGender(partnerDto.getGender());
            }
            if (partnerDto.getLanguage() != null) {
                entity.setLanguage(partnerDto.getLanguage());
            }
            if (partnerDto.getMaritalStatus() != null) {
                entity.setMaritalStatus(partnerDto.getMaritalStatus());
            }
            if (partnerDto.getTitle() != null) {
                entity.setTitle(partnerDto.getTitle());
            }
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            entity.setCreationDate(new Date());
            entity.setCreatedBy(getUser());
            return entityIdToDto(partnerRepository.save(entity));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void edit(PersonDto object) {

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
            } else {
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
    public PartnerDto get(String id) throws PartnerNotFound {
        try {
            PartnerDto object = null;
            PartnerEntity partner = partnerRepository.getById(id);
            if (partner != null) {
                object = entityToObject(partner);
            }
            return object;
        } catch (Exception exception) {
            throw new PartnerNotFound();
        }
    }

    private PartnerDto entityToObject(PartnerEntity partner) {
        PartnerDto object = new PartnerDto();
        object.setId(partner.getId());
        object.setName1(partner.getName1());
        object.setName2(partner.getName2());
        object.setName3(partner.getName3());
        object.setNumber(partner.getNo());
        if (partner.getBirthDate() != null) {
            object.setBirthDate(Conversion.dateToString(partner.getBirthDate()));
        }
        object.setStatus(StringConversion.capitalizeFully(partner.getStatus()));
        if (partner.getTitle() != null) {
            String title = fieldOptionService.getOptionalFieldDescription("TITLE", partner.getTitle());
            object.setTitle(title);
        }
        if (partner.getGender() != null) {
            String gender = fieldOptionService.getOptionalFieldDescription("GENDER", partner.getGender());
            object.setGender(gender);
        }
        if (partner.getMaritalStatus() != null) {
            String maritalStatus = fieldOptionService.getOptionalFieldDescription("MARITAL-STATUS", partner.getMaritalStatus());
            object.setMaritalStatus(maritalStatus);
        }
        if (partner.getLanguage() != null) {
            String language = fieldOptionService.getOptionalFieldDescription("LANGUAGE", partner.getLanguage());
            object.setLanguage(language);
        }
        object.setValidFrom(Conversion.dateToString(partner.getValidFrom()));
        object.setType(StringConversion.capitalizeFully(partner.getType()));
        object.setValidTo(Conversion.dateToString(partner.getValidTo()));
        PartnerIdentityEntity partnerIdentity = getPartnerIdentityNo(partner.getId());
        if (partnerIdentity != null) {
             object.setIdType(fieldOptionService.getOptionalFieldDescription("ID-TYPE", partnerIdentity.getPartnerIdentityPK().getType()));
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

    private AddressDto getAddress(AddressDto address) {
        try {
            PartnerAddressPKEntity partnerAddressPK = new PartnerAddressPKEntity();
            partnerAddressPK.setAddressUsage(address.getType());
            partnerAddressPK.setPartner(address.getPartner());
            List<PartnerAddressEntity> addresses = partnerAddressRepository.findPartnerAddressByPartner(address.getPartner());
            for (PartnerAddressEntity addr : addresses) {
                if (addr.getPartnerAddressPK().getAddressUsage().equals(address.getType())) {
                    String addressId = Integer.toString(addr.getPartnerAddressPK().getAddressId());
                    AddressEntity adr = addressRepository.getById(addressId);
                    if (adr != null) {
                        address.setLine1(adr.getAddressLine1());
                        address.setLine2(adr.getAddressLine2());
                        address.setLine3(adr.getAddressLine3());
                        address.setLine4(adr.getAddressLine4());
                        address.setPostalCode(adr.getPostalCode());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return address;
    }

    private PartnerAddressDto getAddressID(AddressDto address) {
        PartnerAddressDto objects = new PartnerAddressDto();
        try {
            PartnerAddressPKEntity partnerAddressPK = new PartnerAddressPKEntity();
            partnerAddressPK.setAddressUsage(address.getType());
            partnerAddressPK.setPartner(address.getPartner());

            //PartnerAddress partnerAddress = addressUsageController.findPartnerAddress(partnerAddressPK);
            List<PartnerAddressEntity> addresses = partnerAddressRepository.findPartnerAddressByPartner(address.getPartner());
            for (PartnerAddressEntity addr : addresses) {
                if (addr.getPartnerAddressPK().getAddressUsage().equals(address.getType())) {
                    String addressId = Integer.toString(addr.getPartnerAddressPK().getAddressId());
                    AddressEntity adr = addressRepository.getById(addressId);
                    if (adr != null) {
                        objects.setId(adr.getId());
                        objects.setPatner(address.getPartner());
                        objects.setType(address.getType());

                    }
                    break;
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return objects;
    }

    @Override
    public ArrayList<PartnerDto> search(PartnerQueryDto pq) {
        ArrayList<PartnerDto> finalList = new ArrayList<>();
        ArrayList<PartnerDto> filteredList = new ArrayList<>();
        ArrayList<PartnerDto> initialList = new ArrayList<>();
        boolean pass = false;

        if (pq == null) {
            List<PartnerEntity> partnerList = partnerRepository.findAll();
            for (PartnerEntity partner : partnerList) {
                finalList.add(entityToObject(partner));
            }
            return finalList;
        }

        if(pq.getType() != null)
        {
            ProspectSearchDto searchDto = new ProspectSearchDto();
            searchDto.setPartnerType(pq.getType());
            Sort sort = Sort.by("id").descending();
            List<PartnerEntity> partners = partnerRepository.findAll(findByCriteria(searchDto), sort);
            for (PartnerEntity partnerType : partners) {

                    initialList.add(entityToObject(partnerType));

            }

        }
        if (pq.getRole() != null) {
            List<PartnerRoleEntity> partnerRoleList = partnerRoleRepository.findPartnerByRole(pq.getRole());
            for (PartnerRoleEntity partnerRole : partnerRoleList) {
                Optional<PartnerEntity> partner = partnerRepository.findById(partnerRole.getPartnerRolePK().getId());
                PartnerEntity partnerEntity = partner.orElse(null);
                if (partnerEntity != null) {
                    initialList.add(entityToObject(partnerEntity));
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
                    pass = true;
                    //continue;
                }
            }

            if (pq.getIdNumber() != null && !"".equals(pq.getIdNumber())) {
                if (!pqr.getIdNumber().equals(pq.getIdNumber())) {
                    pass = true;
                    //continue;
                }
            }

            if (pq.getName1() != null && !"".equals(pq.getName1())) {
                if (!pqr.getName1().equals(pq.getName1())) {
                    pass = true;
                    //continue;
                }
            }

            if (pq.getName2() != null && !"".equals(pq.getName2())) {
                if (!pqr.getName2().equals(pq.getName2())) {
                    pass = true;
                    //continue;
                }
            }
            if (pq.getName3() != null && !"".equals(pq.getName3())) {
                if (!pqr.getName3().equals(pq.getName3())) {
                    pass = true;
                    //continue;
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

    @Override
    public ArrayList<AddressDto> getAddresses(String partner) {
        ArrayList<AddressDto> partnerAddresses = new ArrayList<>();
        try {

            List<PartnerAddressEntity> addresses = partnerAddressRepository.findPartnerAddressByPartner(partner);
            for (PartnerAddressEntity addr : addresses) {
                String addressId = Integer.toString(addr.getPartnerAddressPK().getAddressId());
                AddressEntity adr = addressRepository.getById(addressId);
                if (adr != null) {
                    AddressDto address = new AddressDto();
                    address.setType(addr.getPartnerAddressPK().getAddressUsage());
                    address.setLine1(adr.getAddressLine1());
                    address.setLine2(adr.getAddressLine2());
                    address.setLine3(adr.getAddressLine3());
                    address.setLine4(adr.getAddressLine4());
                    address.setPostalCode(adr.getPostalCode());
                    address.setId(adr.getId());
                    address.setTypeDescription(fieldOptionService.getOptionalFieldDescription("ADDRESSTYPE", address.getType()));
                    address.setLine3Description(fieldOptionService.getOptionalFieldDescription("SUBURB", adr.getAddressLine3()));
                    address.setLine4Description(fieldOptionService.getOptionalFieldDescription("TOWN", adr.getAddressLine4()));
                    partnerAddresses.add(address);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return partnerAddresses;
    }

    @Override
    public ArrayList<IdentityDto> getIdentities(String partner) {
        ArrayList<IdentityDto> partnerIdentities = new ArrayList<>();
        List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByPartner(partner);
        for (PartnerIdentityEntity partnerIdentity : identityList) {
            IdentityDto identity = new IdentityDto();
            identity.setIdType(partnerIdentity.getPartnerIdentityPK().getType());
            identity.setIdNumber(partnerIdentity.getPartnerIdentityPK().getValue());
            identity.setPartner(partner);
            identity.setValidFrom(Conversion.dateTimeToString(partnerIdentity.getValidFrom()));
            identity.setValidTo(Conversion.dateTimeToString(partnerIdentity.getValidTo()));
            identity.setTypeDescription(fieldOptionService.getFieldOptionDescription("ID-TYPE", identity.getIdType()));

            partnerIdentities.add(identity);
        }
        return partnerIdentities;
    }

    @Override
    public ArrayList<String> getRoles(String id) {
        ArrayList<String> partnerRoles = new ArrayList<>();
        List<PartnerRoleEntity> roleList = partnerRoleRepository.findRoleByPartner(id);
        for (PartnerRoleEntity partnerRole : roleList) {
            partnerRoles.add(partnerRole.getPartnerRolePK().getRole());
        }
        return partnerRoles;
    }

    @Override
    public boolean addRole(String partner, String role) {
        boolean added = false;
        ArrayList<String> roles = getRoles(partner);
        for (String rol : roles) {
            if (rol.equals(role)) {
                added = true;
            }
        }
        if (!added) {
            try {
                PartnerRolePKEntity partnerRolePK = new PartnerRolePKEntity();
                partnerRolePK.setId(partner);
                partnerRolePK.setRole(role);
                PartnerRoleEntity partnerRole = new PartnerRoleEntity();
                partnerRole.setPartnerRolePK(partnerRolePK);
                partnerRole.setValidFrom(new Date());
                partnerRole.setValidTo(Conversion.stringToDate(Constant.END_DATE));
                partnerRoleRepository.save(partnerRole);
                added = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return added;
    }

    @Override
    public boolean addIdentity(IdentityDto identity) {

        try {
            for(PartnerIdentityEntity identityEntity:partnerIdentityRepository.findPartnerIdentityByPartner(identity.getPartner())){
                if(identityEntity.getPartnerIdentityPK().getType().equals(identity.getIdType())){
                    throw new RuntimeException("Duplicate identity type found for partner");
                }
            }
            PartnerIdentityPKEntity partnerIdentityPK = new PartnerIdentityPKEntity();
            partnerIdentityPK.setValue(identity.getIdNumber());
            partnerIdentityPK.setType(identity.getIdType());
            PartnerIdentityEntity partnerIdentity = new PartnerIdentityEntity();
            partnerIdentity.setPartner(identity.getPartner());
            partnerIdentity.setValidFrom(new Date());
            partnerIdentity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            partnerIdentity.setPartnerIdentityPK(partnerIdentityPK);
            partnerIdentityRepository.save(partnerIdentity);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean addContact(ContactDto contact) {
        boolean created = false;
        try {
            PartnerContactPKEntity partnerContactPK = new PartnerContactPKEntity();
            partnerContactPK.setPartner(contact.getPartner());
            partnerContactPK.setType(contact.getType());

            PartnerContactEntity partnerContact = new PartnerContactEntity();
            partnerContact.setPartnerContactPK(partnerContactPK);
            partnerContact.setValue(contact.getValue());
            partnerContact.setValidFrom(new Date());
            partnerContact.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            partnerContactRepository.save(partnerContact);
            created = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return created;
    }

    @Override
    public boolean addAddress(AddressDto address) {
        boolean created = false;
        try {

            AddressEntity adr = new AddressEntity();
            adr.setAddressLine1(address.getLine1());
            adr.setAddressLine2(address.getLine2());
            adr.setAddressLine3(address.getLine3());
            adr.setAddressLine4(address.getLine4());
            adr.setPostalCode(address.getPostalCode());
            adr.setValidFrom(new Date());
            adr.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            adr = addressRepository.save(adr);

            PartnerAddressPKEntity partnerAddressPK = new PartnerAddressPKEntity();
            partnerAddressPK.setAddressUsage(address.getType());
            partnerAddressPK.setPartner(address.getPartner());
            partnerAddressPK.setAddressId(adr.getId());
            PartnerAddressEntity partnerAddress = new PartnerAddressEntity();
            partnerAddress.setPartnerAddressPK(partnerAddressPK);

            partnerAddress.setValidFrom(new Date());
            partnerAddress.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            partnerAddressRepository.save(partnerAddress);

            created = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return created;
    }

    @Override
    public boolean addRelation(RelationDto relation) {
        boolean created = false;
        try {
            PartnerRelationPKEntity partnerRelationPK = new PartnerRelationPKEntity();
            partnerRelationPK.setPartner1(relation.getPartner1());
            partnerRelationPK.setPartner2(relation.getPartner2());
            partnerRelationPK.setType(relation.getType());

            PartnerRelationEntity partnerRelation = new PartnerRelationEntity();
            partnerRelation.setPartnerRelationPK(partnerRelationPK);
            partnerRelation.setValidFrom(new Date());
            partnerRelation.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            partnerRelationRepository.save(partnerRelation);
            created = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return created;
    }

    @Override
    public boolean editRole(RoleDto role) {
        return false;
    }

    @Override
    public boolean editIdentity(IdentityDto idnt) {
        boolean edited = false;
        try {
            List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByPartner(idnt.getPartner());
            if (identityList != null) {
                for (PartnerIdentityEntity partnerIdentity : identityList) {
                    String validFrom = Conversion.dateToString(partnerIdentity.getValidFrom());
                    if (partnerIdentity.getPartnerIdentityPK().getType().equals(idnt.getIdType())
                            && validFrom.equals(idnt.getValidFrom())) {
                        PartnerIdentityPKEntity partneridentityPK = new PartnerIdentityPKEntity();
                        partneridentityPK.setType(idnt.getIdType());
                        partneridentityPK.setValue(partnerIdentity.getPartnerIdentityPK().getValue());

                        partnerIdentityRepository.deleteById(partneridentityPK);
                        partneridentityPK.setType(idnt.getIdType());
                        partneridentityPK.setValue(idnt.getIdNumber());
                        if (idnt.getValidTo() != null) {
                            partnerIdentity.setValidTo(Conversion.stringToDate(idnt.getValidTo()));
                        } else {
                            partnerIdentity.setValidTo(partnerIdentity.getValidTo());
                        }

                        partnerIdentity.setValidFrom(Conversion.stringToDate(validFrom));
                        partnerIdentity.setPartner(idnt.getPartner());
                        partnerIdentity.setPartnerIdentityPK(partneridentityPK);

                        partnerIdentityRepository.save(partnerIdentity);
                        edited = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return edited;
    }

    @Override
    public boolean editContact(ContactDto contact) {
        boolean created = false;
        try {
            PartnerContactPKEntity partnerContactPK = new PartnerContactPKEntity();
            partnerContactPK.setPartner(contact.getPartner());
            partnerContactPK.setType(contact.getType());
            PartnerContactEntity partnerContact = partnerContactRepository.getById(partnerContactPK);
            if (partnerContact != null) {

                if (contact.getValue() != null) {
                    partnerContact.setValue(contact.getValue());
                    partnerContact.setValidFrom(new Date());
                    partnerContact.setValidTo(Conversion.stringToDate(Constant.END_DATE));
                    partnerContactRepository.save(partnerContact);
                    created = true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return created;
    }

    @Override
    public boolean editAddress(AddressDto adrs) {
        boolean edited = false;
        try {
            PartnerAddressEntity adres = new PartnerAddressEntity();
            AddressEntity entityAdress;
            PartnerAddressPKEntity partnerAddressPK = new PartnerAddressPKEntity();
            List<PartnerAddressEntity> addresses = partnerAddressRepository.findPartnerAddressByPartner(adrs.getPartner());
            for (PartnerAddressEntity addr : addresses) {
                if (addr.getPartnerAddressPK().getAddressId() == adrs.getId()) {
                    partnerAddressPK.setAddressUsage(addr.getPartnerAddressPK().getAddressUsage());
                    partnerAddressPK.setPartner(addr.getPartnerAddressPK().getPartner());
                    partnerAddressPK.setAddressId(adrs.getId());
                    partnerAddressRepository.deleteById(partnerAddressPK);
                    break;
                }
            }

            partnerAddressPK.setAddressUsage(adrs.getType());
            partnerAddressPK.setPartner(adrs.getPartner());
            partnerAddressPK.setAddressId(adrs.getId());
            adres.setPartnerAddressPK(partnerAddressPK);

            String address = Integer.toString(adres.getPartnerAddressPK().getAddressId());
            entityAdress = addressRepository.getById(address);
            entityAdress.setAddressLine1(adrs.getLine1());
            entityAdress.setAddressLine2(adrs.getLine2());
            entityAdress.setAddressLine3(adrs.getLine3());
            entityAdress.setAddressLine4(adrs.getLine4());
            entityAdress.setPostalCode(adrs.getPostalCode());

            addressRepository.save(entityAdress);
            adres.setValidFrom(entityAdress.getValidFrom());
            adres.setValidTo(entityAdress.getValidTo());
            partnerAddressRepository.save(adres);
            edited = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return edited;
    }

    @Override
    public boolean editRelation(RelationDto rltn) {
        return false;
    }

    @Override
    public boolean removeIdentity(PartnerIdentityPKEntity pkEntity) {
        try {
            partnerIdentityRepository.deleteById(pkEntity);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean removeContact(ContactDto cntct) {
        boolean removed = false;
        try {
            PartnerContactPKEntity partnerContactPK = new PartnerContactPKEntity();
            partnerContactPK.setPartner(cntct.getPartner());
            partnerContactPK.setType(cntct.getType());
            partnerContactRepository.deleteById(partnerContactPK);
            removed = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return removed;
    }

    @Override
    public boolean removeAddress(PartnerAddressPKEntity pkEntity) throws Exception{
        try {
            addressRepository.deleteById(Integer.toString(pkEntity.getAddressId()));
            partnerAddressRepository.deleteById(pkEntity);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeRelation(RelationDto rltn) {
        boolean removed = false;
        try {
            PartnerRelationPKEntity partnerRelationPK = new PartnerRelationPKEntity();
            partnerRelationPK.setPartner1(rltn.getPartner1());
            partnerRelationPK.setPartner2(rltn.getPartner2());
            partnerRelationPK.setType(rltn.getType());
            partnerRelationRepository.deleteById(partnerRelationPK);
            removed = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return removed;
    }

    @Override
    public boolean archive(String id) {
        boolean archive = false;
        try {
            PartnerEntity partner = partnerRepository.getById(id);
            if (partner != null) ;
            {
                if (!partner.getStatus().equals(Status.INACTIVE)) {
                    partner.setStatus(Status.INACTIVE);
                    partner.setStatusReason(StatusReason.ARCHIVE);
                    List<PartnerRoleEntity> roleList = partnerRoleRepository.findRoleByPartner(partner.getId());
                    for (PartnerRoleEntity partnerRole : roleList) {
                        removeRole(partnerRole.getPartnerRolePK().getId(), partnerRole.getPartnerRolePK().getRole());
                    }
                    partnerRepository.save(partner);
                    archive = true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return archive;
    }

    @Override
    public boolean unArchive(String id) {
        boolean unArchive = false;
        try {
            PartnerEntity partner = partnerRepository.getById(id);
            if (partner != null) {
                if (partner.getStatus().equals(Status.INACTIVE)) {
                    partner.setStatus(Status.ACTIVE);
                    partner.setStatusReason(null);
                    partnerRepository.save(partner);
                    unArchive = true;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return unArchive;
    }

    @Override
    public ArrayList<RelationDto> getRelations(String partner) {
        ArrayList<RelationDto> relations = new ArrayList<>();
        List<PartnerRelationEntity> relationsPartner = partnerRelationRepository.findPartnerRelationByPartner1(partner);
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
    public ArrayList<PartnerRoleDto> getAllRoles() {
        ArrayList<PartnerRoleDto> partnerRoles = new ArrayList<>();
        List<PartnerRoleEntity> roleList = partnerRoleRepository.findAll();
        for (PartnerRoleEntity partnerRole : roleList) {
            PartnerRoleDto partner = new PartnerRoleDto();
            if (partnerRole != null) {
                partner.setPartnerID(partnerRole.getPartnerRolePK().getId());
                partner.setRole(partnerRole.getPartnerRolePK().getRole());
            }
            partnerRoles.add(partner);
        }
        return partnerRoles;
    }

    @Override
    public boolean addBankAccount(PartnerBankAccountDto partnerBankAccount) {
        boolean added = false;
        try {
            PartnerBankingDetailsEntity partnerBankDetails = new PartnerBankingDetailsEntity();
            PartnerBankingDetailsPKEntity partnerBankDetailsPK = new PartnerBankingDetailsPKEntity();
            if (partnerBankAccount.getPartner() != null && partnerBankAccount.getAccountNumber() != null && partnerBankAccount.getType() != null) {

                partnerBankDetailsPK.setAccountNumber(partnerBankAccount.getAccountNumber());
                partnerBankDetailsPK.setPartner(partnerBankAccount.getPartner());
                partnerBankDetailsPK.setType(partnerBankAccount.getType());
                partnerBankDetails.setPartnerBankingDetailsPk(partnerBankDetailsPK);

                if (partnerBankAccount.getAccountHolder() != null) {
                    partnerBankDetails.setAccountHolder(partnerBankAccount.getAccountHolder());

                } else {
                    PartnerEntity partner = partnerRepository.getById(partnerBankAccount.getPartner());
                    if (partner != null) {

                        partnerBankDetails.setAccountHolder(partner.getName1());
                    }
                }

                if (partnerBankAccount.getAccountType() != null) {

                    partnerBankDetails.setAccountType(partnerBankAccount.getAccountType());
                }

                if (partnerBankAccount.getBankName() != null) {
                    partnerBankDetails.setBankName(partnerBankAccount.getBankName());

                }
                if (partnerBankAccount.getBranchCode() != null) {
                    partnerBankDetails.setBranchCode(partnerBankAccount.getBranchCode());

                }

                if (partnerBankAccount.getBranchName() != null) {

                    partnerBankDetails.setBranchName(partnerBankAccount.getBranchName());
                }

                partnerBankDetails.setValidFrom(new Date());

                if (partnerBankAccount.getValidTo() != null) {
                    partnerBankDetails.setValidTo(Conversion.stringToDate(partnerBankAccount.getValidTo()));

                } else {
                    partnerBankDetails.setValidTo(Conversion.stringToDate(Constant.END_DATE));

                }

                partnerBankDetails.setStatus(Status.ACTIVE);
                partnerBankAccountRepository.save(partnerBankDetails);
                added = true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return added;
    }

    @Override
    public ArrayList<PartnerBankAccountDto> getBankAccounts(String partner) {
        List<PartnerBankingDetailsEntity> bankDetails = partnerBankAccountRepository.findPartnerBankByPartner(partner);
        ArrayList<PartnerBankAccountDto> bankingDetails = new ArrayList<>();
        if (!bankDetails.isEmpty()) {
            for (PartnerBankingDetailsEntity bankDetail : bankDetails) {

                PartnerBankAccountDto partnerBankObj = new PartnerBankAccountDto();
                if (bankDetail.getPartnerBankingDetailsPk().getAccountNumber() != null) {
                    partnerBankObj.setAccountNumber(bankDetail.getPartnerBankingDetailsPk().getAccountNumber());
                }

                if (bankDetail.getPartnerBankingDetailsPk().getPartner() != null) {
                    partnerBankObj.setPartner(bankDetail.getPartnerBankingDetailsPk().getPartner());
                }

                if (bankDetail.getPartnerBankingDetailsPk().getType() != null) {

                    partnerBankObj.setType(bankDetail.getPartnerBankingDetailsPk().getType());

                }

                bankingDetails.add(getBankAccount(partnerBankObj));
            }
        }
        return bankingDetails;
    }

    @Override
    public ArrayList<PartnerBankAccountDto> searchBankAccounts(PartnerBankAccountDto partnerBankObj) {
        return null;
    }

    @Override
    public boolean editBankAccount(PartnerBankAccountDto partnerBankAccount) {
        boolean edited = false;
        try {
            PartnerBankingDetailsPKEntity partnerBankDetailsPK = new PartnerBankingDetailsPKEntity();
            partnerBankDetailsPK.setAccountNumber(partnerBankAccount.getAccountNumber());
            partnerBankDetailsPK.setPartner(partnerBankAccount.getPartner());
            partnerBankDetailsPK.setType(partnerBankAccount.getType());
            PartnerBankingDetailsEntity partnerBankDetails = partnerBankAccountRepository.getById(partnerBankDetailsPK);
            if (partnerBankDetails != null) {
                partnerBankDetails.setPartnerBankingDetailsPk(partnerBankDetailsPK);

                if (partnerBankAccount.getAccountHolder() != null) {

                    partnerBankDetails.setAccountHolder(partnerBankAccount.getAccountHolder());
                }

                if (partnerBankAccount.getAccountType() != null) {
                    partnerBankDetails.setAccountType(partnerBankAccount.getAccountType());
                }

                if (partnerBankAccount.getBankName() != null) {

                    partnerBankDetails.setBankName(partnerBankAccount.getBankName());
                }

                if (partnerBankAccount.getBranchCode() != null) {
                    partnerBankDetails.setBranchCode(partnerBankAccount.getBranchCode());

                }
                if (partnerBankAccount.getBranchName() != null) {

                    partnerBankDetails.setBranchName(partnerBankAccount.getBranchName());

                }

                if (partnerBankAccount.getValidTo() != null) {

                    partnerBankDetails.setValidTo(Conversion.stringToDate(partnerBankAccount.getValidTo()));

                }

                partnerBankAccountRepository.save(partnerBankDetails);
                edited = true;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return edited;
    }

    @Override
    public PartnerBankAccountDto getBankAccount(PartnerBankAccountDto bankAccount) {
        PartnerBankAccountDto bankAccountObj = new PartnerBankAccountDto();
        if (bankAccount.getAccountNumber() != null && bankAccount.getType() != null && bankAccount.getPartner() != null) {
            PartnerBankingDetailsPKEntity partnerBankDetailsPK = new PartnerBankingDetailsPKEntity();
            partnerBankDetailsPK.setAccountNumber(bankAccount.getAccountNumber());
            partnerBankDetailsPK.setPartner(bankAccount.getPartner());
            partnerBankDetailsPK.setType(bankAccount.getType());
            PartnerBankingDetailsEntity partnerBankDetails = partnerBankAccountRepository.getById(partnerBankDetailsPK);

            if (partnerBankDetails != null) {

                if (partnerBankDetails.getAccountHolder() != null) {
                    bankAccountObj.setAccountHolder(partnerBankDetails.getAccountHolder());

                }
                if (partnerBankDetails.getAccountType() != null) {
                    bankAccountObj.setAccountType(partnerBankDetails.getAccountType());
                }
                if (partnerBankDetails.getBankName() != null) {
                    bankAccountObj.setBankName(partnerBankDetails.getBankName());
                }
                if (partnerBankDetails.getBranchCode() != null) {
                    bankAccountObj.setBranchCode(partnerBankDetails.getBranchCode());
                }

                if (partnerBankDetails.getBranchName() != null) {

                    bankAccountObj.setBranchName(partnerBankDetails.getBranchName());

                }

                if (partnerBankDetails.getValidFrom() != null) {
                    bankAccountObj.setValidFrom(Conversion.dateToString(partnerBankDetails.getValidFrom()));
                }

                if (partnerBankDetails.getValidTo() != null) {

                    bankAccountObj.setValidTo(Conversion.dateToString(partnerBankDetails.getValidTo()));

                }

                if (partnerBankDetails.getPartnerBankingDetailsPk().getAccountNumber() != null) {
                    bankAccountObj.setAccountNumber(partnerBankDetails.getPartnerBankingDetailsPk().getAccountNumber());
                }

                if (partnerBankDetails.getPartnerBankingDetailsPk().getPartner() != null) {
                    bankAccountObj.setPartner(partnerBankDetails.getPartnerBankingDetailsPk().getPartner());
                }

                if (partnerBankDetails.getPartnerBankingDetailsPk().getType() != null) {

                    bankAccountObj.setType(partnerBankDetails.getPartnerBankingDetailsPk().getType());

                }

            }
        }

        return bankAccountObj;
    }
    @Override
    public String addResource(PartnerResourceApiDto partnerResource) throws NumberRangeObjectNotFound {
        String resourceID = numberRangeService.generateNumber(OrderType.RESOURCE_API);
        if (resourceID != null) {
            PartnerRolePKEntity partnerRolePk = new PartnerRolePKEntity();
            partnerRolePk.setRole(RoleType.ORGANIZATION);
            partnerRolePk.setId(partnerResource.getPartnerID());
            PartnerRoleEntity partnerRole = partnerRoleRepository.getById(partnerRolePk);
            if (partnerRole != null) {
                try {
                    PartnerResourceApiEntity partnerResourceApi = new PartnerResourceApiEntity();
                    partnerResourceApi.setResource_id(resourceID);
                    partnerResourceApi.setPartner_no(partnerResource.getPartnerID());
                    partnerResourceApi.setPartner_url(partnerResource.getPartnerUrl());
                    if (partnerResource.getValidFrom() != null) {
                        partnerResourceApi.setValidFrom(Conversion.stringToDate(partnerResource.getValidFrom()));

                    } else {

                        partnerResourceApi.setValidFrom(new Date());
                    }

                    if (partnerResource.getValidTo() != null) {
                        partnerResourceApi.setValidTo(Conversion.stringToDate(partnerResource.getValidTo()));

                    } else {
                        partnerResourceApi.setValidTo(Conversion.stringToDate(Constant.END_DATE));

                    }

                    if (partnerResource.getStatus() != null) {
                        partnerResourceApi.setStatus(partnerResource.getStatus());

                    } else {

                        partnerResourceApi.setStatus(Status.INACTIVE);
                    }

                    if (partnerResource.getStatusReason() != null) {
                        partnerResourceApi.setStatus_reason(partnerResource.getStatusReason());
                    } else {
                        partnerResourceApi.setStatus_reason(StatusReason.INACTIVE);

                    }

                    partnerResourceApi.setPort_number(partnerResource.getPortNumber());

                    if (partnerResource.getResourceName() != null) {

                        partnerResourceApi.setResource_name(partnerResource.getResourceName());

                    }

                    partnerResourceApiRepository.save(partnerResourceApi);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return resourceID;
    }

    @Override
    public ArrayList<PartnerResourceApiResultDto> searchResourcesApi(PartnerResourceApiResultDto partnerResource) {
        ArrayList<PartnerResourceApiResultDto> partnerUrlList = new ArrayList<>();
        if (partnerResource.getPartnerID() != null) {
            List<PartnerResourceApiEntity> partnerResourcesList = partnerResourceApiRepository.findByPartner(partnerResource.getPartnerID());
            if (!partnerResourcesList.isEmpty()) {
                for (PartnerResourceApiEntity partnerResourceObject : partnerResourcesList) {
                    PartnerResourceApiResultDto partnerUrl = getResourceApi(partnerResourceObject.getResource_id());
                    partnerUrlList.add(partnerUrl);
                }
            }
        }
        return partnerUrlList;
    }

    @Override
    public PartnerResourceApiResultDto getResourceApi(String resource_id) {
        PartnerResourceApiResultDto partnerResourceResult = new PartnerResourceApiResultDto();
        if (resource_id != null) {
            PartnerResourceApiEntity partnerResource = partnerResourceApiRepository.getById(resource_id);
            if (partnerResource != null) {
                partnerResourceResult.setResourceID(partnerResource.getResource_id());
                partnerResourceResult.setPartnerID(partnerResource.getPartner_no());
                if (partnerResource.getResource_name() != null) {
                    partnerResourceResult.setPartnerUrl(partnerResource.getPartner_url() + ":" + partnerResource.getPort_number() + partnerResource.getResource_name());
                } else {
                    partnerResourceResult.setPartnerUrl(partnerResource.getPartner_url() + ":" + partnerResource.getPort_number());
                }
                partnerResourceResult.setValidFrom(Conversion.dateToString(partnerResource.getValidFrom()));
                partnerResourceResult.setValidTo(Conversion.dateToString(partnerResource.getValidTo()));
                partnerResourceResult.setStatus(partnerResource.getStatus());
                partnerResourceResult.setStatusReason(partnerResource.getStatus_reason());

            }
        }
        return partnerResourceResult;
    }

    @Override
    public boolean editResourceApi(PartnerResourceApiDto partnerResourceObj) {
        boolean edited = false;
        PartnerResourceApiEntity partnerResourceApi = partnerResourceApiRepository.getById(partnerResourceObj.getId());
        if (partnerResourceApi != null) {
            try {
                if (partnerResourceObj.getPartnerUrl() != null) {
                    partnerResourceApi.setPartner_url(partnerResourceObj.getPartnerUrl());
                }
                if (partnerResourceObj.getPortNumber() != null) {
                    partnerResourceApi.setPort_number(partnerResourceObj.getPortNumber());
                }
                if (partnerResourceObj.getResourceName() != null) {
                    partnerResourceApi.setResource_name(partnerResourceObj.getResourceName());
                }
                if (partnerResourceObj.getStatus() != null) {
                    partnerResourceApi.setStatus(partnerResourceObj.getStatus());
                }
                if (partnerResourceObj.getStatusReason() != null) {
                    partnerResourceApi.setStatus_reason(partnerResourceObj.getStatusReason());
                }
                if (partnerResourceObj.getValidFrom() != null) {
                    partnerResourceApi.setValidFrom(Conversion.stringToDate(partnerResourceObj.getValidFrom()));
                }
                if (partnerResourceObj.getValidTo() != null) {
                    partnerResourceApi.setValidTo(Conversion.stringToDate(partnerResourceObj.getValidTo()));
                }

                partnerResourceApiRepository.save(partnerResourceApi);
                edited = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return edited;
    }

    @Override
    public boolean addAttachment(PartnerAttachmentDto attachment) {
        boolean processed = false;
        try {
            PartnerAttachmentEntity entity = new PartnerAttachmentEntity();
            entity.setId(attachment.getId());
            entity.setPartner(attachment.getParent());
            entity.setType(attachment.getType());
            entity.setName(attachment.getFileName());
            entity.setExtension(attachment.getExtension());
            entity.setCreatedBy(attachment.getCreatedBy());
            entity.setCreatedAt(new Date());
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate("9999-12-31"));
            partnerAttachmentRepository.save(entity);
            processed = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return processed;
    }

    @Override
    public boolean removeAttachment(PartnerAttachmentDto attachment) {
        boolean edited = false;
        PartnerAttachmentEntity attach = partnerAttachmentRepository.getById(attachment.getId());
        if (attach != null) {
            try {
                attach.setStatus(Status.DELETED);
                attach.setValidTo(new Date());
                partnerAttachmentRepository.save(attach);
                edited = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return edited;
    }

    @Override
    public ArrayList<PartnerAttachmentDto> getAttachments(String partner) throws Exception {
        ArrayList<PartnerAttachmentDto> list = new ArrayList<>();
        List<PartnerAttachmentEntity> attachments = partnerAttachmentRepository.findByPartner(partner);
        for (PartnerAttachmentEntity partnerAttachment : attachments) {
            PartnerAttachmentDto object = new PartnerAttachmentDto();
            if (partnerAttachment.getStatus().equals(Status.ACTIVE)) {
                UserDto usrObj = new UserDto();
                object.setId(partnerAttachment.getId());
                object.setParent(partnerAttachment.getPartner());
                object.setCreatedAt(Conversion.dateTimeToString(partnerAttachment.getCreatedAt()));
                object.setCreatedBy(partnerAttachment.getCreatedBy());

                usrObj = userService.getUserByName(partnerAttachment.getCreatedBy());
                if (usrObj.getPartner() != null) {
                    object.setAttachedById(usrObj.getPartner());
                    PartnerDto prt = new PartnerDto();
                    prt = get(usrObj.getPartner());

                    if (prt != null) {
                        PersonDto person = new PersonDto(prt);
                        object.setAttachedBy(person);
                    }
                }
                object.setExtension(partnerAttachment.getExtension());
                object.setFileName(partnerAttachment.getName());
                object.setType(fieldOptionService.getFieldOptionDescription("DOCUMENTTYPE", partnerAttachment.getType()));
                if (partnerAttachment.getStatus() != null) {
                    object.setStatus(partnerAttachment.getStatus());
                }
                if (partnerAttachment.getStatusReason() != null) {
                    object.setStatusReason(partnerAttachment.getStatusReason());
                }
                if (partnerAttachment.getValidFrom() != null) {
                    object.setValidFrom(Conversion.dateToString(partnerAttachment.getValidFrom()));
                }
                if (partnerAttachment.getValidTo() != null) {
                    object.setValidTo(Conversion.dateToString(partnerAttachment.getValidTo()));
                }
                list.add(object);
            }

        }
        return list;
    }

    @Override
    public boolean addDate(PartnerDateDto date) {
        boolean created = false;
        try {
            PartnerDatePKEntity ptnDatePK = new PartnerDatePKEntity();
            ptnDatePK.setPartnerNumber(date.getPartnerNo());
            if (date.getType() != null) {
                ptnDatePK.setType(date.getType());
            }
            PartnerDateEntity ptnDate = new PartnerDateEntity();
            if (ptnDatePK != null) {
                ptnDate.setPartnerDatePK(ptnDatePK);
            }
            if (date.getValue() != null) {
                ptnDate.setValue(Conversion.stringToDate(date.getValue()));
            } else {
                ptnDate.setValue(new Date());
            }
            partnerDateRepository.save(ptnDate);
            created = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return created;
    }

    @Override
    public boolean editDate(PartnerDateDto date) {
        boolean edited = true;
        PartnerDatePKEntity ptnDatePK = new PartnerDatePKEntity();
        try {
            ptnDatePK.setPartnerNumber(date.getPartnerNo());
            ptnDatePK.setType(date.getType());
            PartnerDateEntity ptnDate = new PartnerDateEntity();
            ptnDate.setPartnerDatePK(ptnDatePK);
            ptnDate.setValue(Conversion.stringToDate(date.getValue()));
            partnerDateRepository.save(ptnDate);
            edited = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return edited;
    }

    @Override
    public PartnerDateDto getDate(String partnerNo, String dateType) {
        PartnerDateDto partnerDateObj = null;
        PartnerDatePKEntity partnerDatePK = new PartnerDatePKEntity();
        partnerDatePK.setPartnerNumber(partnerNo);
        partnerDatePK.setType(dateType);
        PartnerDateEntity partnerDate = partnerDateRepository.getById(partnerDatePK);
        if (partnerDate != null) {
            partnerDateObj = new PartnerDateDto();
            partnerDateObj.setPartnerNo(partnerDate.getPartnerDatePK().getPartnerNumber());
            partnerDateObj.setType(partnerDate.getPartnerDatePK().getType());
            if (partnerDateObj.getType() != null) {
                partnerDateObj.setTypeDescription(fieldOptionService.getFieldOptionDescription("DATETYPES", partnerDateObj.getType()));
            }
            partnerDateObj.setValue(Conversion.dateTimeToString(partnerDate.getValue()));
        }
        return partnerDateObj;
    }

    @Override
    public ArrayList<PartnerDateDto> getDates(String partnerNo) {
        ArrayList<PartnerDateDto> dates = new ArrayList<>();
        List<PartnerDateEntity> dateList = partnerDateRepository.findByPartner(partnerNo);
        for (PartnerDateEntity partnerDate : dateList) {
            PartnerDateDto pDates = new PartnerDateDto();
            pDates.setType(partnerDate.getPartnerDatePK().getType());
            if (pDates.getType() != null) {
                pDates.setTypeDescription(fieldOptionService.getFieldOptionDescription("DATETYPES", pDates.getType()));
            }
            pDates.setValue((Conversion.dateTimeToString(partnerDate.getValue())));
            dates.add(pDates);
        }
        return dates;
    }

    @Override
    public ArrayList<PartnerDateDto> getAllDates() {
        ArrayList<PartnerDateDto> dates = new ArrayList<>();
        List<PartnerDateEntity> dateList = partnerDateRepository.findAll();
        for (PartnerDateEntity partnerDate : dateList) {
            PartnerDateDto pDates = new PartnerDateDto();
            pDates.setPartnerNo(partnerDate.getPartnerDatePK().getPartnerNumber());
            pDates.setType(partnerDate.getPartnerDatePK().getType());
            if (pDates.getType() != null) {
                pDates.setTypeDescription(fieldOptionService.getFieldOptionDescription("DATETYPES", pDates.getType()));
            }
            pDates.setValue((Conversion.dateTimeToString(partnerDate.getValue())));
            dates.add(pDates);
        }
        return dates;
    }

    @Override
    public ArrayList<RelationDto> getRelationByPartner1(String partner1) {
        ArrayList<RelationDto> relations = new ArrayList<>();
        List<PartnerRelationEntity> relationsPartner = partnerRelationRepository.findPartnerRelationByPartner1(partner1);
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
    public ArrayList<PartnerDto> getPartners(String partnerRole) {
        ArrayList<PartnerDto> partners = new ArrayList();
        List<PartnerRoleEntity> partnerRoleList = partnerRoleRepository.findPartnerByRole(partnerRole);
        if (!partnerRoleList.isEmpty()) {
            for (PartnerRoleEntity prtRole : partnerRoleList) {
                String fo = fieldOptionService.getFieldOptionDescription("PARTNER-ROLE", prtRole.getPartnerRolePK().getRole());
                if (fo != null) {
                    PartnerDto partner = new PartnerDto();
                    try {
                        partner = get(prtRole.getPartnerRolePK().getId());
                    } catch (PartnerNotFound e) {
//                        throw new RuntimeException(e);
                    }
                    partners.add(partner);
                }
            }
        }
        return partners;
    }

    @Override
    public ProspectDto getProspect(String id) throws DoesNotExist {
        PartnerEntity partner = partnerRepository.getById(id);
        if (partner != null) {
            try {
                return entityToProspect(partner);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new DoesNotExist();
        }
    }

    @Override
    public ArrayList<ProspectDto> getProspects(ProspectSearchDto searchDto) throws Exception {
        ArrayList<ProspectDto> prospectDtoArrayList = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<PartnerEntity> partners = partnerRepository.findAll(findByCriteria(searchDto), sort);
        prospectDtoArrayList = entityArrayToDto(partners);
        return prospectDtoArrayList;
    }

    @Override
    public boolean editProspect(String id, ProspectEditDto editDto) throws DoesNotExist, Exception {
        PartnerEntity entity = partnerRepository.getById(id);
        if (entity != null) {
            try {
                if (editDto.getSurname() != null) {
                    entity.setName1(editDto.getSurname().toUpperCase());
                }
                if (editDto.getOrganisationName() != null) {
                    entity.setName1(editDto.getOrganisationName().toUpperCase());
                }
                if (editDto.getMiddleName() != null) {
                    entity.setName3(editDto.getMiddleName().toUpperCase());
                }
                if (editDto.getFirstName() != null) {
                    entity.setName2(editDto.getFirstName().toUpperCase());
                }
                partnerRepository.save(entity);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new DoesNotExist();
        }
    }

    @Override
    public PartnerDto getOptional(String id) {

        Optional<PartnerEntity> partnerEntity = partnerRepository.findById(id);
        PartnerEntity partner = partnerEntity.orElse(null);
        PartnerDto partnerDto = new PartnerDto();
        if (partner != null) {
            partnerDto.setId(partner.getId());
            partnerDto.setBirthDate(String.valueOf(partner.getBirthDate()));
            partnerDto.setName1(partner.getName1());
            partnerDto.setName2(partner.getName2());
            partnerDto.setName3(partner.getName3());
            partnerDto.setGender(partner.getGender());
            partnerDto.setTitle(partner.getTitle());
            partnerDto.setMaritalStatus(partner.getMaritalStatus());
            partnerDto.setStatus(partner.getStatus());


            PartnerIdentityEntity partnerIdentity = getPartnerIdentityNo(partner.getId());
            if (partnerIdentity != null) {
                partnerDto.setIdType(fieldOptionService.getFieldOptionDescription("ID-TYPE", partnerIdentity.getPartnerIdentityPK().getType()));
                partnerDto.setIdNumber(partnerIdentity.getPartnerIdentityPK().getValue());
            }


        }

        return partnerDto;
    }

    @Override
    public PartnerDto getPartner(String id) {
        Optional<PartnerEntity> partner = partnerRepository.findById(id);
        PartnerDto object = new PartnerDto();
        if (!partner.isEmpty()) {
            object = entityToObject(partner.get());
            return object;
        } else {
            return null;
        }

    }

    @Override
    public ArrayList<ContactDto> getContacts(String partner) {
        List<ContactDto> contactDtos = partnerContactRepository.findContactsByPartner(partner)
                .stream()
                .map(contact -> {
                    ContactDto contactDto = new ContactDto();
                    contactDto.setPartner(contact.getPartnerContactPK().getPartner());
                    contactDto.setType(contact.getPartnerContactPK().getType());
                    contactDto.setValue(contact.getValue());
                    contactDto.setValidFrom(contact.getValidFrom());
                    contactDto.setValidTo(contact.getValidTo());
                    return contactDto;
                })
                .collect(Collectors.toList());
        return (ArrayList<ContactDto>) contactDtos;
    }

    @Override
    public boolean assignRole(String role, String id) throws PartnerNotFound {

        boolean assign = false;

        if (role != null && id != null) {
            PartnerDto partnerDto = get(id);
            PartnerRolePKEntity partnerRolePKEntity = new PartnerRolePKEntity();
            partnerRolePKEntity.setId(partnerDto.getId());
            partnerRolePKEntity.setRole(role);
            PartnerRoleEntity partnerRoleEntity = new PartnerRoleEntity();
            partnerRoleEntity.setPartnerRolePK(partnerRolePKEntity);
            partnerRoleEntity.setValidFrom(new Date());
            partnerRoleEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            partnerRoleRepository.save(partnerRoleEntity);
            assign = true;
        }

        return assign;
    }

    @Override
    public ArrayList<AddressDto> getPartnerAddress(AddressQueryDto queryDto) {
         ArrayList<AddressDto> addressDtos = new ArrayList<>();
        Sort sort = Sort.by("partnerAddressPK").descending();
        List<PartnerAddressEntity> addressEntities = partnerAddressRepository.findAll(findByAddress(queryDto),sort);
        for(PartnerAddressEntity address:addressEntities){
            AddressDto addressDto = new AddressDto();
            addressDto.setPartner(address.getPartnerAddressPK().getPartner());
            addressDto.setType(address.getPartnerAddressPK().getAddressUsage());
            addressDto.setId(address.getPartnerAddressPK().getAddressId());
            String AddressType = fieldOptionService.getOptionalFieldDescription("ADDRESSES", address.getPartnerAddressPK().getAddressUsage());
            if(AddressType != null){
                addressDto.setTypeDescription(AddressType);
            }
            AddressEntity address1 = new AddressEntity();
            address1 = addressRepository.getById(Integer.toString(address.getPartnerAddressPK().getAddressId()));
            addressDto.setLine1(address1.getAddressLine1());
            addressDto.setLine2(address1.getAddressLine2());
            addressDto.setLine3(address1.getAddressLine3());
            String line3 = fieldOptionService.getOptionalFieldDescription("SUBURB", address1.getAddressLine3());
            if(line3 != null){
                addressDto.setLine3Description(line3);
            }
             String line4 = fieldOptionService.getOptionalFieldDescription("TOWN", address1.getAddressLine4());
            if(line4 != null){
                addressDto.setLine4Description(line4);
            }
            addressDto.setLine4(address1.getAddressLine4());
            addressDto.setPostalCode(address1.getPostalCode());

            addressDtos.add(addressDto);
        }
        return addressDtos;
    }

    @Override
    public boolean editPartnerAddress(String id, AddressEditDto addressEditDto) throws Exception {
        try{
            AddressEntity entity = addressRepository.getById(id);
            if(addressEditDto.getLine1() != null && addressEditDto.getLine1() != ""){
                entity.setAddressLine1(addressEditDto.getLine1());
            }
            if(addressEditDto.getLine2() != null && addressEditDto.getLine2() != ""){
                entity.setAddressLine2(addressEditDto.getLine2());
            }
            if(addressEditDto.getLine3() != null && addressEditDto.getLine3() != ""){
                  entity.setAddressLine3(addressEditDto.getLine3());
            }
            if(addressEditDto.getLine4() != null && addressEditDto.getLine4() != ""){
                 entity.setAddressLine4(addressEditDto.getLine4());
            }
            if(addressEditDto.getPostalCode() != null && addressEditDto.getPostalCode() != ""){
                entity.setPostalCode(addressEditDto.getPostalCode());
            }
            addressRepository.save(entity);
            return true;
        }catch (Exception ex){
           throw new RuntimeException(ex);
        }
    }

    @Override
    public ArrayList<ContactGetDto> getPartnerContact(ContactQueryDto queryDto) throws Exception {
        ArrayList<ContactGetDto> contacts = new ArrayList<>();
        Sort sort = Sort.by("partnerContactPK").descending();
        for(PartnerContactEntity contact:partnerContactRepository.findAll(findByContact(queryDto),sort)){
            ContactGetDto contactPartner = new ContactGetDto();
            String contactType = fieldOptionService.getOptionalFieldDescription("CONTACT-TYPE", contact.getPartnerContactPK().getType());
            if(contactType != null){
                contactPartner.setDescription(contactType);
            }
            contactPartner.setType(contact.getPartnerContactPK().getType());
            contactPartner.setPartner(contact.getPartnerContactPK().getPartner());
            contactPartner.setValue(contact.getValue());
            contactPartner.setValidFrom(Conversion.dateToString(contact.getValidFrom()));
            contactPartner.setValidTo(Conversion.dateToString(contact.getValidTo()));
            contacts.add(contactPartner);

        }
        return contacts;
    }

    @Override
    public boolean removePartnerContact(PartnerContactPKEntity pkEntity) throws Exception {
        try{
            partnerContactRepository.deleteById(pkEntity);
            return true;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean addPartnerContact(String id, ContactCreateDto contact) throws Exception {
        try{
            PartnerContactEntity entity = new PartnerContactEntity();
            PartnerContactPKEntity pk = new PartnerContactPKEntity();
            pk.setPartner(id);
            pk.setType(contact.getType());
            entity.setPartnerContactPK(pk);
            entity.setValue(contact.getValue());
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate("9999-12-31"));
            partnerContactRepository.save(entity);
            return true;
        }catch(Exception ex){
          throw new RuntimeException(ex);
        }
    }

    @Override
    public ArrayList<IdentityDto> getPartnerIdentities(IdentityQueryDto queryDto) throws Exception {
        try{
            ArrayList<IdentityDto> identities = new ArrayList<>();
            Sort sort = Sort.by("partner").descending();
            for(PartnerIdentityEntity identity:partnerIdentityRepository.findAll(findByIdentity(queryDto),sort)){
                IdentityDto id = new IdentityDto();
                id.setPartner(identity.getPartner());
                id.setValidFrom(Conversion.dateToString(identity.getValidFrom()));
                id.setValidTo(Conversion.dateToString(identity.getValidTo()));
                id.setIdNumber(identity.getPartnerIdentityPK().getValue());
                id.setIdType(identity.getPartnerIdentityPK().getType());

                id.setTypeDescription(fieldOptionService.getOptionalFieldDescription("ID-TYPE", identity.getPartnerIdentityPK().getType()));
                identities.add(id);
            }
            return identities;
        }catch (Exception ex){
           throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean editPartnerIdentity(IdentityEditDto editDto, String type, String partner) throws Exception {
        try{
           PartnerIdentityEntity entity = partnerIdentityRepository.findPartnerIdentityByTypeAndPartner(type,partner);
            if (entity == null) {
                throw new Exception("Partner identity not found.");
            }
            removeIdentity(entity.getPartnerIdentityPK());

           if(editDto.getIdNumber() != null && editDto.getIdNumber() != ""){
               entity.getPartnerIdentityPK().setValue(editDto.getIdNumber());
           }
           if(editDto.getValidFrom() != null && editDto.getValidFrom() != ""){
               entity.setValidFrom(Conversion.stringToDate(editDto.getValidFrom()));
           }
            if(editDto.getValidTo() != null && editDto.getValidTo() != ""){
               entity.setValidTo(Conversion.stringToDate(editDto.getValidTo()));
            }
            partnerIdentityRepository.save(entity);
            return true;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ArrayList<RoleDto> getPartnerRoles(String id) throws Exception {
        try{
            ArrayList<RoleDto> roles = new ArrayList<>();
            for(PartnerRoleEntity role:partnerRoleRepository.findRoleByPartner(id)){
                RoleDto roleDto = new RoleDto();
                roleDto.setId(role.getPartnerRolePK().getRole());
                roleDto.setDescription(fieldOptionService.getFieldOptionDescription("PARTNER-ROLE",role.getPartnerRolePK().getRole()));
                roleDto.setValidFrom(role.getValidFrom());
                roleDto.setValidTo(role.getValidTo());
                roles.add(roleDto);
            }
            return roles;
        }catch (Exception ex){
           throw new RuntimeException(ex);
        }
    }

    @Override
    public void addPartnersRole(RolePartnerDto rolePartnerDto) throws Exception {
        try{
            PartnerRolePKEntity pkEntity = new PartnerRolePKEntity();
            PartnerRoleEntity entity = new PartnerRoleEntity();
            pkEntity.setRole(rolePartnerDto.getRole());
            pkEntity.setId(rolePartnerDto.getPartner());
            entity.setPartnerRolePK(pkEntity);
            entity.setValidFrom(new Date());
            entity.setValidTo((Conversion.stringToDate(Constant.END_DATE)));
            partnerRoleRepository.save(entity);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean deleteRoles(PartnerRolePKEntity rolePKEntity) throws Exception {
        try{
            partnerRoleRepository.deleteById(rolePKEntity);
            return true;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean editPartner(PartnerEditDto editDto, String id) throws Exception {
        try{
            PartnerEntity entity = partnerRepository.getById(id);
            if(entity != null){
                if(editDto.getGender() != null && editDto.getGender() != ""){
                    entity.setGender(editDto.getGender());
                }
                if(editDto.getMaritalStatus() != null && editDto.getMaritalStatus() != ""){
                    entity.setMaritalStatus(editDto.getMaritalStatus());
                }
                if(editDto.getTitle() != null && editDto.getTitle() != ""){
                    entity.setTitle(editDto.getTitle());
                }
                if(editDto.getLastName() != null && editDto.getLastName() != ""){
                    entity.setName1(editDto.getLastName());
                }
                if(editDto.getFirstName() != null && editDto.getFirstName() != ""){
                    entity.setName2(editDto.getFirstName());
                }
                if(editDto.getMiddleName() != null && editDto.getMiddleName() != ""){
                    entity.setName3(editDto.getMiddleName());
                }
                if(editDto.getDateOfBirth() != null && editDto.getDateOfBirth() != ""){
                    entity.setBirthDate(Conversion.stringToDate(editDto.getDateOfBirth()));
                }
                partnerRepository.save(entity);
                return true;
            }
            else{
                throw new PartnerNotFound();
            }
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }

    }

    private String getUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }

    private PartnerDto entityIdToDto(PartnerEntity partnerEntity) throws Exception {
        try {
            PartnerDto partnerDto = new PartnerDto();
            partnerDto.setId(partnerEntity.getId());
            return partnerDto;

        } catch (Exception e) {
            throw new Exception();
        }
    }

    private ProspectDto entityToProspect(PartnerEntity partnerEntity) throws Exception {
        try {
            ProspectDto prospectDto = new ProspectDto();
            prospectDto.setId(partnerEntity.getId());
            prospectDto.setNumber(partnerEntity.getNo());
            if (partnerEntity.getType() != null) {
                if (partnerEntity.getType().equalsIgnoreCase(PartnerType.ORGANISATION)) {
                    prospectDto.setOrganisationName(partnerEntity.getName1());
                } else {
                    prospectDto.setFirstName(partnerEntity.getName2());
                    prospectDto.setSurname(partnerEntity.getName1());
                    if (partnerEntity.getName3() != null) {
                        prospectDto.setMiddleName(partnerEntity.getName3());
                    }
                }
            }
            prospectDto.setStatus(partnerEntity.getStatus());
            prospectDto.setValidTo(Conversion.dateToString(partnerEntity.getValidTo()));
            prospectDto.setValidFrom(Conversion.dateToString(partnerEntity.getValidFrom()));
            return prospectDto;
        } catch (Exception e) {
            throw new Exception();
        }
    }

    private ArrayList<ProspectDto> entityArrayToDto(List<PartnerEntity> partners) throws Exception {
        ArrayList<ProspectDto> prospectDto = new ArrayList<>();
        for (PartnerEntity partner : partners) {
            try {
                prospectDto.add(entityToProspect(partner));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return prospectDto;
    }

    private Specification<PartnerIdentityEntity> findByIdentity(IdentityQueryDto queryDto){
        return(root,query,cb) ->{
            Predicate predicate = cb.conjunction();
            if(queryDto.getPartner() != null) {
                predicate = cb.and(predicate,cb.equal(root.get("partner"),queryDto.getPartner()));
            }
            if(queryDto.getValue() != null){
                predicate = cb.and(predicate,cb.equal(root.get("partnerIdentityPK").get("value"),queryDto.getValue()));
            }
            if(queryDto.getType() != null){
                predicate = cb.and(predicate,cb.equal(root.get("partnerIdentityPK").get("type"),queryDto.getType()));
            }
            return predicate;
        };
    }

    private Specification<PartnerContactEntity> findByContact(ContactQueryDto queryDto){
        return(root,query,cb) ->{
            Predicate predicate = cb.conjunction();
            if(queryDto.getPartner() != null){
                predicate = cb.and(predicate,cb.equal(root.get("partnerContactPK").get("partner"),queryDto.getPartner()));
            }
            if(queryDto.getType() != null){
                predicate = cb.and(predicate,cb.equal(root.get("partnerContactPK").get("type"),queryDto.getType()));
            }
            if(queryDto.getValue() != null){
                predicate = cb.and(predicate,cb.equal(root.get("value"),queryDto.getValue()));
            }
            return predicate;
        };
    }
    private Specification<PartnerAddressEntity> findByAddress(AddressQueryDto queryAddress){
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryAddress.getPartnerId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerAddressPK").get("partner"), queryAddress.getPartnerId()));
            }
            if (queryAddress.getType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerAddressPK").get("addressUsage"), queryAddress.getType()));
            }
            return predicate;
        };
    }
    private Specification<PartnerEntity> findByCriteria(ProspectSearchDto prospectSearch) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (prospectSearch.getPartnerNumber() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("no"), prospectSearch.getPartnerNumber()));
            }
            if (prospectSearch.getPartnerType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), prospectSearch.getPartnerType()));
            }
            if (prospectSearch.getSurname() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("name1"), prospectSearch.getSurname()));
            }
            if (prospectSearch.getOrganisationName() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("name1"), prospectSearch.getOrganisationName()));
            }
            if (prospectSearch.getMiddleName() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("name3"), prospectSearch.getMiddleName()));
            }
            if (prospectSearch.getFirstName() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("name2"), prospectSearch.getFirstName()));
            }
            return predicate;
        };
    }
}