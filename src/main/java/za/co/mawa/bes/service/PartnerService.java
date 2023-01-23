package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.RelationDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.dao.PartnerDao;
import za.co.mawa.bes.entity.PartnerRelationEntity;
import za.co.mawa.bes.entity.PartnerRolePKEntity;
import za.co.mawa.bes.repository.PartnerRelationRepository;
import za.co.mawa.bes.repository.PartnerRoleRepository;
import za.co.mawa.bes.repository.RoleRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class PartnerService implements PartnerDao {

    @Autowired
    PartnerRoleRepository partnerRoleRepository;
    @Autowired
    PartnerRelationRepository partnerRelationRepository;
    @Override
    public String create(PartnerEntity partnerEntity) {
        return null;
    }

    @Override
    public PartnerEntity findById(String id) {
        return null;
    }

    @Override
    public PartnerDto get(String id) {
        return null;
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
}
