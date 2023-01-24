package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.raretag.mawabes.dto.PartnerQueryDto;
import za.co.mawa.bes.dao.PartnerDao;

import java.util.ArrayList;

@Service
public class PartnerService implements PartnerDao {
    @Override
    public String create(PartnerEntity partnerEntity) {
        return null;
    }

    @Override
    public PartnerEntity findById(String id) {
        return null;
    }

    @Override
    public ArrayList<PartnerDto> search(PartnerQueryDto pq) {
        return null;
    }
}
