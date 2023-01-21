package za.co.mawa.bes.dao;

import za.co.mawa.bes.entity.PartnerEntity;

public interface PartnerDao {
    String create(PartnerEntity partnerEntity);
    PartnerEntity findById(String id);
}
