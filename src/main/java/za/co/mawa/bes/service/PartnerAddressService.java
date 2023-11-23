package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.AddressDto;
import za.co.mawa.bes.dto.partner.PartnerAddressCreateDto;
import za.co.mawa.bes.dto.partner.PartnerAddressDeleteDto;
import za.co.mawa.bes.dto.partner.PartnerAddressDto;
import za.co.mawa.bes.entity.PartnerAddressEntity;
import za.co.mawa.bes.entity.PartnerAddressPKEntity;
import za.co.mawa.bes.repository.PartnerAddressRepository;
import za.co.mawa.bes.utils.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PartnerAddressService {
    @Autowired
    PartnerAddressRepository partnerAddressRepository;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;

    @Autowired
    AddressService addressService;

    public void add(PartnerAddressCreateDto partnerAddressCreateDto) {
        try {
            PartnerAddressPKEntity partnerAddressPKEntity = new PartnerAddressPKEntity();
            partnerAddressPKEntity.setAddressId(partnerAddressCreateDto.getId());
            partnerAddressPKEntity.setPartner(partnerAddressCreateDto.getPartner());
            partnerAddressPKEntity.setAddressUsage(partnerAddressCreateDto.getType());
            PartnerAddressEntity partnerAddressEntity = new PartnerAddressEntity();
            partnerAddressEntity.setPartnerAddressPK(partnerAddressPKEntity);
            partnerAddressEntity.setValidFrom(new Date());
            partnerAddressRepository.save(partnerAddressEntity);
        } catch (Exception exception) {
        }
    }

    public List<PartnerAddressDto> get(String partner) {
        List<PartnerAddressDto> partnerAddressDtoList = new ArrayList<>();
        try {
            List<PartnerAddressEntity> partnerAddressEntities = partnerAddressRepository.findByPartner(partner);
            for (PartnerAddressEntity partnerAddressEntity : partnerAddressEntities) {
                PartnerAddressDto partnerAddressDto = new PartnerAddressDto();
                partnerAddressDto.setPartner(partnerService.get(partnerAddressEntity.getPartnerAddressPK().getPartner()));
                partnerAddressDto.setAddress(addressService.get(partnerAddressEntity.getPartnerAddressPK().getAddressId()));
                partnerAddressDto.setType(fieldOptionService.getFieldOption(Field.ADDRESS_TYPE, partnerAddressEntity.getPartnerAddressPK().getAddressUsage()));
                partnerAddressDtoList.add(partnerAddressDto);
            }
        } catch (Exception exception) {

        }
        return partnerAddressDtoList;
    }

    public void delete(PartnerAddressDeleteDto partnerAddressDeleteDto) {
        try {
            PartnerAddressPKEntity partnerAddressPKEntity = new PartnerAddressPKEntity();
            partnerAddressPKEntity.setAddressId(partnerAddressDeleteDto.getAddressId());
            partnerAddressPKEntity.setPartner(partnerAddressDeleteDto.getPartner());
            partnerAddressPKEntity.setAddressUsage(partnerAddressDeleteDto.getType());
            partnerAddressRepository.deleteById(partnerAddressPKEntity);
        } catch (Exception exception) {
        }
    }
}
