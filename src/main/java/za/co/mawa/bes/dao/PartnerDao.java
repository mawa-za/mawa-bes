package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.raretag.mawabes.dto.PartnerQueryDto;


import java.util.ArrayList;

public interface PartnerDao {
    String create(PartnerEntity partnerEntity);
    PartnerEntity findById(String id);
    ArrayList<PartnerDto> search(PartnerQueryDto pq);
}
