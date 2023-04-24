package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.prospect.ProspectDto;
import za.co.mawa.bes.dto.prospect.ProspectEditDto;
import za.co.mawa.bes.dto.prospect.ProspectSearchDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.dto.PartnerQueryDto;
import za.co.mawa.bes.exception.PartnerNotFound;

import java.util.ArrayList;

public interface PartnerDao {
    //String create(PartnerEntity partnerEntity);
    PartnerDto create(PartnerDto object);
    void edit(PersonDto object);
    PartnerEntity findById(String id);
    PartnerDto get (String id) throws PartnerNotFound;
    boolean removeRole(String partner, String role);
    ArrayList<RelationDto> getRelationByPartner2(String partner2);
    ArrayList<PartnerDto> search(PartnerQueryDto pq);
    ArrayList<AddressDto> getAddresses(String partner);
    ArrayList<IdentityDto> getIdentities(String partner);
    ArrayList<String> getRoles(String id);
    boolean addRole(String partner, String role);
    boolean addIdentity(IdentityDto identity);
    boolean addContact(ContactDto contact);
    boolean addAddress(AddressDto address);
    boolean addRelation(RelationDto relation);
    boolean editRole(RoleDto role);
    boolean editIdentity(IdentityDto idnt);
    boolean editContact(ContactDto contact);
    boolean editAddress(AddressDto adrs);
    boolean editRelation(RelationDto rltn);
    boolean removeIdentity(IdentityDto idnt);
    boolean removeContact(ContactDto cntct);
    boolean removeAddress(AddressDto adrs);
    boolean removeRelation(RelationDto rltn);
    boolean archive(String id);
    boolean unArchive(String id);
    ArrayList<RelationDto> getRelations(String partner);
    ArrayList<PartnerRoleDto> getAllRoles();
    boolean addBankAccount(PartnerBankAccountDto partnerBankAccount);
    ArrayList<PartnerBankAccountDto> getBankAccounts(String partner);
    ArrayList<PartnerBankAccountDto> searchBankAccounts(PartnerBankAccountDto partnerBankObj);
    boolean editBankAccount(PartnerBankAccountDto partnerBankAccount);
    PartnerBankAccountDto getBankAccount(PartnerBankAccountDto bankAccount);
    ArrayList<ValueDto> getPartnerRoles(String partner);
    String addResource(PartnerResourceApiDto partnerResource) throws NumberRangeObjectNotFound;
    ArrayList<PartnerResourceApiResultDto> searchResourcesApi(PartnerResourceApiResultDto partnerResource);
    PartnerResourceApiResultDto getResourceApi(String resource_id);
    boolean editResourceApi(PartnerResourceApiDto partnerResourceObj);
    boolean addAttachment(PartnerAttachmentDto attachment);
    boolean removeAttachment(PartnerAttachmentDto attachment);
    ArrayList<PartnerAttachmentDto> getAttachments(String partner) throws Exception;
    boolean addDate(PartnerDateDto date);
    boolean editDate(PartnerDateDto date);
    PartnerDateDto getDate(String partnerNo, String dateType);
    ArrayList<PartnerDateDto> getDates(String partnerNo);
    ArrayList<PartnerDateDto> getAllDates();
    ArrayList<RelationDto> getRelationByPartner1(String partner1);
    ArrayList<PartnerDto> getPartners (String partnerRole);
    ProspectDto getProspect(String id) throws DoesNotExist;
    ArrayList<ProspectDto> getProspects(ProspectSearchDto searchDto) throws Exception;
    boolean editProspect(String id, ProspectEditDto editDto) throws DoesNotExist,Exception;

    PartnerDto getOptional(String id);
    PartnerDto getPartner(String id);

    ArrayList<ContactDto> getContacts(String partner);

    boolean assignRole(String role, String id) throws PartnerNotFound;
}
