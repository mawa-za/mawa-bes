package za.co.mawa.bes.service;


import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.TenantDao;
import za.co.mawa.bes.dto.JwtRequest;
import za.co.mawa.bes.dto.TenantCreateDto;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.TenantPropertyDto;
import za.co.mawa.bes.dto.partner.PartnerCreateDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.TenantEntity;
import za.co.mawa.bes.entity.TenantPropertyEntity;
import za.co.mawa.bes.entity.TenantPropertyPKEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.TenantPropertyRepository;
import za.co.mawa.bes.repository.TenantRepository;
import za.co.mawa.bes.utils.PartnerType;
import za.co.mawa.bes.utils.Status;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class TenantService implements TenantDao {
    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    TenantPropertyRepository tenantPropertyRepository;
    @Autowired
    SettingService settingService;
    @Autowired
    PartnerService partnerService;
    @Override
    public TenantDto create(TenantDto tenantDto) throws Exception {
        try {
            TenantEntity tenantEntity = new TenantEntity();
            tenantEntity.setName(tenantDto.getName());
            tenantEntity.setHost(tenantDto.getHost());
            tenantEntity.setUrl(tenantDto.getUrl());
            tenantEntity.setStatus(Status.ACTIVE);
            tenantDto.setId(tenantRepository.save(tenantEntity).getId());
            return tenantDto;
        } catch (Exception exception) {
            throw new Exception();
        }
    }


    @Override
    public List<TenantDto> getAll() {
        List<TenantDto> tenantDtos = new ArrayList<>();
        List<TenantEntity> tenantEntities = tenantRepository.findAll();
        for(TenantEntity tenantEntity: tenantEntities){
            TenantDto tenantDto = new TenantDto();
            tenantDto.setId(tenantEntity.getId());
            tenantDto.setHost(tenantEntity.getHost());
            tenantDto.setName(tenantEntity.getName());
            tenantDto.setUrl(tenantEntity.getUrl());
            tenantDto.setStatus(tenantEntity.getStatus());
            tenantDtos.add(tenantDto);
        }
        return tenantDtos;
    }


    @Override
    public void addProperty(TenantPropertyDto propertyDto) {
        try {
            TenantPropertyPKEntity tenantPropertyPKEntity = new TenantPropertyPKEntity();
            TenantPropertyEntity tenantPropertyEntity = new TenantPropertyEntity();
            tenantPropertyPKEntity.setTenant(propertyDto.getTenant());
            tenantPropertyPKEntity.setProperty(propertyDto.getProperty());
            tenantPropertyEntity.setTenantPropertyPKEntity(tenantPropertyPKEntity);
            tenantPropertyEntity.setValue(propertyDto.getValue());
            tenantPropertyRepository.save(tenantPropertyEntity);
        } catch (Exception exception) {
        }
    }

    @Transactional
    @Override
    public Properties getTenantProperties(String tenant) {
        Properties properties = new Properties();
        List<TenantPropertyEntity> propertyEntities = tenantPropertyRepository.findAll();
        for (TenantPropertyEntity tenantPropertyEntity : propertyEntities) {
            if (tenantPropertyEntity.getTenantPropertyPKEntity().getTenant().equals(tenant)) {
                properties.put(tenantPropertyEntity.getTenantPropertyPKEntity().getProperty(), tenantPropertyEntity.getValue());
            }
        }
        return properties;
    }

    public PartnerDto addTenantPartner(TenantCreateDto createDto,String tenant){
        try{
           PartnerCreateDto partner = new PartnerCreateDto();
           partner.setName1(createDto.getTenantName());
           partner.setType(PartnerType.TENANT);
           PartnerDto partnerTenant = partnerService.create(partner);
           if(partnerTenant.getId() != null){
               TenantPropertyDto propertyDto = new TenantPropertyDto();
               propertyDto.setTenant(tenant);
               propertyDto.setValue(partnerTenant.getId());
               propertyDto.setProperty("PARTNER-ID");
               addProperty(propertyDto);
           };
            return  partnerTenant;
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    };
}
