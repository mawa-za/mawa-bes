package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.AddressCreateDto;
import za.co.mawa.bes.dto.AddressDto;
import za.co.mawa.bes.dto.AddressEditDto;
import za.co.mawa.bes.dto.partner.PartnerAddressDto;
import za.co.mawa.bes.entity.AddressEntity;
import za.co.mawa.bes.entity.PartnerAddressEntity;
import za.co.mawa.bes.entity.PartnerAddressPKEntity;
import za.co.mawa.bes.repository.AddressRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AddressService {
    @Autowired
    AddressRepository addressRepository;
    @Autowired
    FieldOptionService fieldOptionService;

    public AddressDto add(AddressCreateDto addressCreateDto) {
        try {
            AddressEntity addressEntity = new AddressEntity();
            addressEntity.setAddressLine1(addressCreateDto.getLine1());
            addressEntity.setAddressLine2(addressCreateDto.getLine2());
            addressEntity.setAddressLine3(addressCreateDto.getLine3());
            addressEntity.setAddressLine4(addressCreateDto.getLine4());
            addressEntity.setPostalCode(addressCreateDto.getPostalCode());
            addressEntity.setValidFrom(new Date());
            addressEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            addressEntity = addressRepository.save(addressEntity);
            AddressDto addressDto = new AddressDto();
            addressDto.setId(addressEntity.getId());
            return addressDto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AddressDto get(String id) {
        try {
            AddressEntity addressEntity = addressRepository.getById(id);
            AddressDto addressDto = new AddressDto();
            addressDto.setId(addressEntity.getId());
            addressDto.setLine1(addressEntity.getAddressLine1());
            addressDto.setLine2(addressEntity.getAddressLine2());
            addressDto.setLine3(fieldOptionService.getFieldOption(Field.SUBURB, addressEntity.getAddressLine3()));
            addressDto.setLine4(fieldOptionService.getFieldOption(Field.CITY, addressEntity.getAddressLine4()));
            addressDto.setPostalCode(addressEntity.getPostalCode());
            return addressDto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void edit(AddressEditDto addressEditDto) {
        try {
            AddressEntity addressEntity = addressRepository.getById(addressEditDto.getId());
            addressEntity.setAddressLine1(addressEditDto.getLine1());
            addressEntity.setAddressLine2(addressEditDto.getLine2());
            addressEntity.setAddressLine3(addressEditDto.getLine3());
            addressEntity.setAddressLine4(addressEditDto.getLine4());
            addressEntity.setPostalCode(addressEditDto.getPostalCode());
            addressEntity.setValidFrom(new Date());
            addressRepository.save(addressEntity);
        } catch (Exception exception) {
        }
    }
    public void delete(String id) {
        try {
           addressRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
