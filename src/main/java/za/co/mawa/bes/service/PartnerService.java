package za.co.raretag.mawabes.service;

import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dto.PartnerDto;
import za.co.raretag.mawabes.dto.PartnerQueryDto;
import za.co.raretag.mawabes.entity.PartnerEntity;
import za.co.raretag.mawabes.dao.PartnerDao;

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
