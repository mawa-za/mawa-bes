package za.co.raretag.mawabes.model;

import za.co.raretag.mawabes.entity.PartnerEntity;

public interface PartnerDao {
    String create(PartnerEntity partnerEntity);
    PartnerEntity findById(String id);
}
