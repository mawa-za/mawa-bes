package za.co.mawa.bes.service;


import com.nimbusds.jose.shaded.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.TenantDao;
import za.co.mawa.bes.dto.JwtRequest;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.TenantPropertyDto;
import za.co.mawa.bes.entity.TenantEntity;
import za.co.mawa.bes.entity.TenantPropertyEntity;
import za.co.mawa.bes.entity.TenantPropertyPKEntity;
import za.co.mawa.bes.repository.TenantPropertyRepository;
import za.co.mawa.bes.repository.TenantRepository;
import za.co.mawa.bes.utils.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class TenantAdminService implements TenantDao {
    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    TenantPropertyRepository tenantPropertyRepository;
    @Autowired
    SettingService settingService;

    Gson gson = new Gson();
    @Value("${admin-api-url}")
    private String adminApiUrl;
    @Value("${admin-username}")
    private String adminApiUsername;
    @Value("${admin-password}")
    private String adminApiPassword;
    private static final String ADMIN_API_URL = "admin-api-url";
    private static final String ADMIN_USERNAME = "admin-username";
    private static final String ADMIN_PASSWORD = "admin-password";
    private static final String ADMIN_SETTING = "ADMIN";
    private String getAdminToken() {
        Properties properties = settingService.getSettings(ADMIN_SETTING);
        JwtRequest tokenRequest = new JwtRequest();
        tokenRequest.setUsername(adminApiUsername);
        tokenRequest.setPassword(adminApiPassword);
        String token = "";
        try {
            URL url = new URL(adminApiUrl + "/Authenticate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(gson.toJson(tokenRequest));
            writer.close();
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                token += output;
            }
            conn.disconnect();
        } catch (MalformedURLException ex) {

        } catch (IOException ex) {

        }
        return token;
    }
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
        List<TenantDto> tenants = new ArrayList<>();
        String jsonString = "";
        try {
            String token = getAdminToken();
            URL url = new URL( adminApiUrl + "/tenant");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                jsonString += output;
            }
            conn.disconnect();
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                TenantDto tenantDto = gson.fromJson(String.valueOf(array.getJSONObject(i)), TenantDto.class);
                tenants.add(tenantDto);
            }



        } catch (MalformedURLException ex) {

        } catch (IOException ex) {

        }
        return tenants;
    }

    @Override
    public Properties getTenantProperties(String tenant) {
        Properties props = new Properties();
        String jsonString = "";
        try {
            String token = getAdminToken();
            URL url = new URL(settingService.getSettings(ADMIN_SETTING).get(ADMIN_API_URL).toString() + "/tenant");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                jsonString += output;
            }
            conn.disconnect();

        } catch (MalformedURLException ex) {

        } catch (IOException ex) {

        }
        return props;
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


}
