package za.co.raretag.mawabes.service;

import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.entity.PartnerEntity;
import za.co.raretag.mawabes.model.PartnerDao;
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
}
