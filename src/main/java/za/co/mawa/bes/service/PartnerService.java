package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.dao.PartnerDao;
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
