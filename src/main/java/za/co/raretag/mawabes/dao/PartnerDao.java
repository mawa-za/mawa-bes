package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.entity.PartnerEntity;

public interface PartnerDao {
    String create(PartnerEntity partnerEntity);
    PartnerEntity findById(String id);
}
