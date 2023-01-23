package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.RelationDto;
import za.co.mawa.bes.entity.PartnerEntity;

import java.util.ArrayList;

public interface PartnerDao {
    String create(PartnerEntity partnerEntity);
    PartnerEntity findById(String id);
    PartnerDto get (String id);
    boolean removeRole(String partner, String role);
    ArrayList<RelationDto> getRelationByPartner2(String partner2);
}
