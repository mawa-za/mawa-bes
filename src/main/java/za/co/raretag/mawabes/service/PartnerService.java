package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.PartnerDao;
import za.co.raretag.mawabes.dto.PartnerDto;
import za.co.raretag.mawabes.dto.RelationDto;
import za.co.raretag.mawabes.entity.PartnerEntity;
import za.co.raretag.mawabes.repository.PartnerRepository;

import java.util.ArrayList;

@Service
public class PartnerService implements PartnerDao {
    @Autowired
    PartnerRepository partnerRepository;
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
        PartnerDto object = null;
        PartnerEntity partner = partnerRepository.findById(id).get();
        if(partner != null){
            object = entityToObject(partner);
        }
        return object;
    }

    @Override
    public PartnerDto entityToObject(PartnerEntity partner) {
        return null;
    }

    @Override
    public ArrayList<RelationDto> getRelationByPartner1(String partner1) {
        return null;
    }
}
