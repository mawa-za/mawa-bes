package za.co.mawa.bes.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.partner.*;
import za.co.mawa.bes.dto.prospect.ProspectDto;
import za.co.mawa.bes.dto.prospect.ProspectEditDto;
import za.co.mawa.bes.dto.prospect.ProspectSearchDto;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PartnerServiceV2 {

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
    PartnerAttributeRepository partnerAttributeRepository;
    //    @Autowired
//    UserService userService;
    @Autowired
    PartnerDateRepository partnerDateRepository;
    @Autowired
    PartnerIdentityService partnerIdentityService;
    @Autowired
    PartnerViewRepository partnerViewRepository;

    public PartnerDto create(PartnerInboundDto partnerInboundDto) {

        try {
            PartnerEntity entity = new PartnerEntity();
            String partnerNo = numberRangeService.generateNumber(partnerInboundDto.getPartnerType());
            entity.setNo(partnerNo);
            entity.setType(partnerInboundDto.getPartnerType().toUpperCase());
            if (partnerInboundDto.getName1() != null) {
                entity.setName1(partnerInboundDto.getName1().toUpperCase());
            }
            if (partnerInboundDto.getName2() != null) {
                entity.setName2(partnerInboundDto.getName2().toUpperCase());
            }
            if (partnerInboundDto.getName3() != null) {
                entity.setName3(partnerInboundDto.getName3().toUpperCase());
            }
            if (partnerInboundDto.getBirthDate() != null) {
                entity.setBirthDate(partnerInboundDto.getBirthDate());
            }
            if (partnerInboundDto.getGender() != null) {
                entity.setGender(partnerInboundDto.getGender());
            }
            if (partnerInboundDto.getLanguage() != null) {
                entity.setLanguage(partnerInboundDto.getLanguage());
            }
            if (partnerInboundDto.getMaritalStatus() != null) {
                entity.setMaritalStatus(partnerInboundDto.getMaritalStatus());
            }
            if (partnerInboundDto.getTitle() != null) {
                entity.setTitle(partnerInboundDto.getTitle());
            }
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            entity.setCreationDate(new Date());
            entity.setCreatedBy(getUser());
            entity = partnerRepository.save(entity);
            return get(entity.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void edit(PartnerEditDto partnerEditDto) {

        try {
            PartnerEntity partner = partnerRepository.getById(partnerEditDto.getId());
            if (partner != null) {
                if (partnerEditDto.getName1() != null) {
                    partner.setName1(partnerEditDto.getName1().toUpperCase());
                }
                if (partnerEditDto.getName2() != null) {
                    partner.setName2(partnerEditDto.getName2().toUpperCase());
                }

                if (partnerEditDto.getName3() != null) {
                    partner.setName3(partnerEditDto.getName3().toUpperCase());
                }
                if (partnerEditDto.getBirthDate() != null) {
                    partner.setBirthDate(partnerEditDto.getBirthDate());
                }
                if (partnerEditDto.getGender() != null) {
                    partner.setGender(partnerEditDto.getGender());
                }
                if (partnerEditDto.getLanguage() != null) {
                    partner.setLanguage(partnerEditDto.getLanguage());
                }
                if (partnerEditDto.getMaritalStatus() != null) {
                    partner.setMaritalStatus(partnerEditDto.getMaritalStatus());
                }
                if (partnerEditDto.getTitle() != null) {
                    partner.setTitle(partnerEditDto.getTitle());
                }
                if (partnerEditDto.getStatus() != null) {
                    partner.setStatus(partnerEditDto.getStatus());
                }
                partnerRepository.save(partner);
            } else {
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PartnerDto get(String id) throws PartnerNotFoundException {
        try {
            PartnerEntity partner = partnerRepository.getById(id);
            PartnerDto partnerDto = new PartnerDto();
            partnerDto.setId(partner.getId());
            partnerDto.setNumber(partner.getNo());
            partnerDto.setIdentity(partnerIdentityService.get(id));
            partnerDto.setName1(partner.getName1());
            partnerDto.setName2(partner.getName2());
            partnerDto.setName3(partner.getName3());
            partnerDto.setBirthDate(partner.getBirthDate());
            partnerDto.setTitle(fieldOptionService.getFieldOption(Field.TITLE, partner.getTitle()));
            partnerDto.setType(fieldOptionService.getFieldOption(Field.PARTNER_TYPE, partner.getType()));
            partnerDto.setStatus(fieldOptionService.getFieldOption(Field.PARTNER_STATUS, partner.getStatus()));
            partnerDto.setGender(fieldOptionService.getFieldOption(Field.GENDER, partner.getGender()));
            partnerDto.setMaritalStatus(fieldOptionService.getFieldOption(Field.MARITAL_STATUS, partner.getMaritalStatus()));
            partnerDto.setLanguage(fieldOptionService.getFieldOption(Field.LANGUAGE, partner.getLanguage()));
            partnerDto.setValidFrom(partner.getValidFrom());
            partnerDto.setValidTo(partner.getValidTo());
            return partnerDto;
        } catch (Exception exception) {
            throw new PartnerNotFoundException();
        }
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

    public ArrayList<PartnerViewEntity> searchByString(String query) {
        List<PartnerViewEntity> partnerViewEntities = partnerViewRepository.findByString(query);
        return new ArrayList<>(partnerViewEntities);
    }

    public List<PartnerViewEntity> getAllPartnersUsingView(PartnerQueryDto partnerQueryDto) {
        Set<PartnerViewEntity> partnerViewEntities = new HashSet<>();
        try {
            List<PartnerViewEntity> allPartners = partnerViewRepository.findAllOrderedByPartnerNo();

            for (PartnerViewEntity entity : allPartners) {
                boolean matches = true;

                if (partnerQueryDto.getType() != null && !partnerQueryDto.getType().isEmpty()) {
                    matches = matches && partnerQueryDto.getType().equals(entity.getPartnerType());
                }
                if (partnerQueryDto.getRole() != null && !partnerQueryDto.getRole().isEmpty()) {
                    matches = matches && partnerQueryDto.getRole().equals(entity.getPartnerRole());
                }
                if (partnerQueryDto.getAttributeName() != null && !partnerQueryDto.getAttributeName().isEmpty()) {
                    String attributeValue = partnerQueryDto.getAttributeValue();
                    String attribute = getAttributeValueByName(entity, partnerQueryDto.getAttributeName());
                    matches = matches && attributeValue.equals(attribute);
                }

                if (matches) {
                    partnerViewEntities.add(entity);
                }
            }
        } catch (Exception e) {
        }

        return new ArrayList<>(partnerViewEntities);
    }

    private String getAttributeValueByName(PartnerViewEntity entity, String attributeName) {
        return switch (attributeName) {
            case "identityNumber" -> entity.getIdentityNumber();
            case "name1" -> entity.getName1();
            case "name2" -> entity.getName2();
            case "name3" -> entity.getName3();
            case "partnerRole" -> entity.getPartnerRole();
            case "partnerType" -> entity.getPartnerType();
            default -> null;
        };
    }
    public ArrayList<String> getRoles(String id) {
        ArrayList<String> partnerRoles = new ArrayList<>();
        List<PartnerRoleEntity> roleList = partnerRoleRepository.findRoleByPartner(id);
        for (PartnerRoleEntity partnerRole : roleList) {
            partnerRoles.add(partnerRole.getPartnerRolePK().getRole());
        }
        return partnerRoles;
    }

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


    public boolean editRole(RoleDto role) {
        return false;
    }

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


    public boolean editRelation(RelationDto rltn) {
        return false;
    }


    public boolean removeIdentity(PartnerIdentityPKEntity pkEntity) {
        try {
            partnerIdentityRepository.deleteById(pkEntity);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


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


    public void archive(String id) {
        try {
            PartnerEntity partner = partnerRepository.getById(id);
            if (partner != null) ;
            {
                partner.setStatus(Status.INACTIVE);
                partner.setStatusReason(StatusReason.ARCHIVE);
                partnerRepository.save(partner);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void unArchive(String id) {
        try {
            PartnerEntity partner = partnerRepository.getById(id);
            if (partner != null) {
                partner.setStatus(Status.ACTIVE);
                partner.setStatusReason(null);
                partnerRepository.save(partner);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


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

    public boolean addAttachment(PartnerAttachmentEntity attachment) {
        try {
            partnerAttachmentRepository.save(attachment);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


    public boolean removeAttachment(PartnerAttachmentPKEntity attachment) {
        try {
            partnerAttachmentRepository.deleteById(attachment);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


    public ArrayList<PartnerAttachmentDto> getAttachments(String partner) throws Exception {
        try {
            ArrayList<PartnerAttachmentDto> partnerAttachments = new ArrayList<>();
            for (PartnerAttachmentEntity attachment : partnerAttachmentRepository.findByPartner(partner)) {
                PartnerAttachmentDto attachmentDto = new PartnerAttachmentDto();
                attachmentDto.setPartner(attachment.getPartnerAttachmentPKEntity().getPartner());
                attachmentDto.setDocumentType(attachment.getPartnerAttachmentPKEntity().getDocumentType());
                attachmentDto.setFileId(attachment.getFileId());
                attachmentDto.setStatus(attachment.getStatus());
                attachmentDto.setValidFrom(Conversion.dateToString(attachment.getValidFrom()));
                attachmentDto.setValidTo(Conversion.dateToString(attachment.getValidTo()));
                partnerAttachments.add(attachmentDto);
            }
            return partnerAttachments;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

    }


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
                    } catch (PartnerNotFoundException e) {
//                        throw new RuntimeException(e);
                    }
                    partners.add(partner);
                }
            }
        }
        return partners;
    }


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


    public ArrayList<ProspectDto> getProspects(ProspectSearchDto searchDto) throws Exception {
        ArrayList<ProspectDto> prospectDtoArrayList = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<PartnerEntity> partners = partnerRepository.findAll(findByCriteria(searchDto), sort);
        prospectDtoArrayList = entityArrayToDto(partners);
        return prospectDtoArrayList;
    }


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


    public PartnerDto getOptional(String id) throws PartnerNotFoundException {
        return get(id);
    }

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


    public boolean assignRole(String role, String id) throws PartnerNotFoundException {

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


    public ArrayList<ContactGetDto> getPartnerContact(ContactQueryDto queryDto) throws Exception {
        ArrayList<ContactGetDto> contacts = new ArrayList<>();
        Sort sort = Sort.by("partnerContactPK").descending();
        for (PartnerContactEntity contact : partnerContactRepository.findAll(findByContact(queryDto), sort)) {
            ContactGetDto contactPartner = new ContactGetDto();
            String contactType = fieldOptionService.getOptionalFieldDescription("CONTACT-TYPE", contact.getPartnerContactPK().getType());
            if (contactType != null) {
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


    public boolean removePartnerContact(PartnerContactPKEntity pkEntity) throws Exception {
        try {
            partnerContactRepository.deleteById(pkEntity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public boolean addPartnerContact(String id, ContactCreateDto contact) throws Exception {
        try {
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ArrayList<RoleDto> getPartnerRoles(String id) throws Exception {
        try {
            ArrayList<RoleDto> roles = new ArrayList<>();
            for (PartnerRoleEntity role : partnerRoleRepository.findRoleByPartner(id)) {
                RoleDto roleDto = new RoleDto();
                roleDto.setId(role.getPartnerRolePK().getRole());
                roleDto.setDescription(fieldOptionService.getFieldOptionDescription("PARTNER-ROLE", role.getPartnerRolePK().getRole()));
                roleDto.setValidFrom(role.getValidFrom());
                roleDto.setValidTo(role.getValidTo());
                roles.add(roleDto);
            }
            return roles;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public void addPartnersRole(RolePartnerDto rolePartnerDto) throws Exception {
        try {
            PartnerRolePKEntity pkEntity = new PartnerRolePKEntity();
            PartnerRoleEntity entity = new PartnerRoleEntity();
            pkEntity.setRole(rolePartnerDto.getRole());
            pkEntity.setId(rolePartnerDto.getPartner());
            entity.setPartnerRolePK(pkEntity);
            entity.setValidFrom(new Date());
            entity.setValidTo((Conversion.stringToDate(Constant.END_DATE)));
            partnerRoleRepository.save(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public boolean deleteRoles(PartnerRolePKEntity rolePKEntity) throws Exception {
        try {
            partnerRoleRepository.deleteById(rolePKEntity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ArrayList<PartnerAttribute> getAttributes(PartnerAttributeQueryDto queryDto) {
        try {
            ArrayList<PartnerAttribute> attributes = new ArrayList<>();
            Sort sort = Sort.by("partnerAttributePKEntity").descending();
            for (PartnerAttributeEntity attributeEntity : partnerAttributeRepository.findAll(findByAttribute(queryDto), sort)) {
                PartnerAttribute attribute = new PartnerAttribute();
                attribute.setAttribute(attributeEntity.getPartnerAttributePKEntity().getAttribute());
                attribute.setPartner(attributeEntity.getPartnerAttributePKEntity().getPartner());
                attribute.setValue(attributeEntity.getValue());
                attribute.setValidFrom(Conversion.dateToString(attributeEntity.getValidFrom()));
                attribute.setValidTo(Conversion.dateToString(attributeEntity.getValidTo()));
                attributes.add(attribute);
            }
            return attributes;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public boolean addAttribute(PartnerAttributeCreateDto createDto) throws Exception {
        try {
            PartnerAttributeEntity entity = new PartnerAttributeEntity();
            PartnerAttributePKEntity pk = new PartnerAttributePKEntity();
            pk.setAttribute(createDto.getAttribute());
            pk.setPartner(createDto.getPartner());
            entity.setPartnerAttributePKEntity(pk);
            entity.setValue(createDto.getValue());
            entity.setValidTo(new Date());
            entity.setValidFrom(Conversion.stringToDate("9999-12-31"));
            partnerAttributeRepository.save(entity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean editAttribute(PartnerAttributeEditDto editDto, String partner, String attribute) throws Exception {
        try {
            PartnerAttributePKEntity pkEntity = new PartnerAttributePKEntity();
            pkEntity.setAttribute(attribute);
            pkEntity.setPartner(partner);
            PartnerAttributeEntity entity = partnerAttributeRepository.getById(pkEntity);
            if (editDto.getValidFrom() != null) {
                entity.setValidFrom(Conversion.stringToDate(editDto.getValidFrom()));
            }
            if (editDto.getValidTo() != null) {
                entity.setValidTo(Conversion.stringToDate(editDto.getValidTo()));
            }
            if (editDto.getValue() != null) {
                entity.setValue(editDto.getValue());
            }
            partnerAttributeRepository.save(entity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public boolean deleteAttribute(PartnerAttributePKEntity pkEntity) throws Exception {
        try {
            partnerAttributeRepository.deleteById(pkEntity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean contactEdit(PartnerContactPKEntity entity, ContactEditDto editDto) throws Exception {
        try {
            PartnerContactEntity contactEntity = partnerContactRepository.getById(entity);
            if (editDto.getValue() != null && editDto.getValue() != "") {
                contactEntity.setValue(editDto.getValue());
            }
            if (editDto.getValidFrom() != null && editDto.getValidFrom() != "") {
                contactEntity.setValidFrom(Conversion.stringToDate(editDto.getValidFrom()));
            }
            if (editDto.getValidTo() != null && editDto.getValidTo() != "") {
                contactEntity.setValidTo(Conversion.stringToDate(editDto.getValidTo()));
            }
            partnerContactRepository.save(contactEntity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    private String getUser() {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String currentUser = userDetails.getUsername();
            return currentUser;
        } catch (Exception ex) {
            return "SYSTEM";
        }
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
                if (partnerEntity.getType().equalsIgnoreCase(PartnerType.ORGANIZATION)) {
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


    private Specification<PartnerContactEntity> findByContact(ContactQueryDto queryDto) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryDto.getPartner() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerContactPK").get("partner"), queryDto.getPartner()));
            }
            if (queryDto.getType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerContactPK").get("type"), queryDto.getType()));
            }
            if (queryDto.getValue() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("value"), queryDto.getValue()));
            }
            return predicate;
        };
    }

    private Specification<PartnerAddressEntity> findByAddress(AddressQueryDto queryAddress) {
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

    private Specification<PartnerAttributeEntity> findByAttribute(PartnerAttributeQueryDto queryDto) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (queryDto.getPartner() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerAttributePKEntity").get("partner"), queryDto.getPartner()));
            }
            if (queryDto.getAttribute() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partnerAttributePKEntity").get("attribute"), queryDto.getAttribute()));
            }
            if (queryDto.getValue() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("value"), queryDto.getValue()));
            }
            if (queryDto.getValidTo() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("validTo"), queryDto.getValidTo()));
            }
            if (queryDto.getValidFrom() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("validFrom"), queryDto.getValidFrom()));
            }
            return predicate;

        };
    }

    public PartnerBasicDto partnerDtoToPartnerBasicDto(PartnerEntity partner) {
        PartnerBasicDto partnerBasicDto = new PartnerBasicDto();
        partnerBasicDto.setId(partner.getId());
        partnerBasicDto.setType(partner.getType());
        partnerBasicDto.setName1(partner.getName1());
        partnerBasicDto.setName2(partner.getName2());
        partnerBasicDto.setName3(partner.getName3());
        partnerBasicDto.setTitle(partner.getTitle());
        partnerBasicDto.setStatus(partner.getStatus());
        partnerBasicDto.setGender(partner.getGender());
        return partnerBasicDto;
    }

    public String getFullName(PartnerDto partner) {
        if (partner == null) {
            return "";
        }

        List<String> names = new ArrayList<>();
        if (partner.getName1() != null && !partner.getName1().isEmpty()) {
            names.add(partner.getName1());
        }
        if (partner.getName2() != null && !partner.getName2().isEmpty()) {
            names.add(partner.getName2());
        }
        if (partner.getName3() != null && !partner.getName3().isEmpty()) {
            names.add(partner.getName3());
        }
        if (partner.getName4() != null && !partner.getName4().isEmpty()) {
            names.add(partner.getName4());
        }

        // Joining names with a single space
        return String.join(" ", names).trim();
    }

}