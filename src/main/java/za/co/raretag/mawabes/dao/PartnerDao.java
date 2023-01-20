package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.PartnerDto;
import za.co.raretag.mawabes.dto.PartnerQueryDto;
import za.co.raretag.mawabes.entity.PartnerEntity;

import java.util.ArrayList;

public interface PartnerDao {
    String create(PartnerEntity partnerEntity);
    PartnerEntity findById(String id);
    ArrayList<PartnerDto> search(PartnerQueryDto pq);
}
