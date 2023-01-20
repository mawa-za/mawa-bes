package za.co.mawa.bes.service;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.JwtRequest;
import za.co.mawa.bes.dao.TenantDao;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

@Service
public class TenantService implements TenantDao {
    @Autowired
    SettingService settingService;

    Gson gson = new Gson();

    private static final String ADMIN_API_URL = "admin-api-url";
    private static final String ADMIN_USERNAME = "admin-username";
    private static final String ADMIN_PASSWORD = "admin-password";
    private static final String ADMIN_SETTING = "ADMIN";
    @Override
    public String getAdminToken() {
        Properties properties = settingService.getSettings(ADMIN_SETTING);
        JwtRequest tokenRequest = new JwtRequest();
        tokenRequest.setUsername(properties.get(ADMIN_USERNAME).toString());
        tokenRequest.setPassword(properties.get(ADMIN_PASSWORD).toString());
        String token = "";
        try {
            URL url = new URL(properties.get(ADMIN_API_URL).toString() + "/Authenticate");
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
    public Properties getTenantProperties(String tenant) {
        Properties props = new Properties();
        String jsonString = "";
        try {
            String token = getAdminToken();
            URL url = new URL(settingService.getSettings(ADMIN_SETTING).get(ADMIN_API_URL).toString() + "/tenant/"+tenant+"property");
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


//        Properties properties = new Properties();
//        List<TenantPropertyEntity> propertyEntities = tenantPropertyRepository.findAll();
//        for (TenantPropertyEntity tenantPropertyEntity : propertyEntities) {
//            if (tenantPropertyEntity.getTenant().equals(tenant)) {
//                properties.put(tenantPropertyEntity.getProperty(), tenantPropertyEntity.getValue());
//            }
//        }
//        return properties;
    }

    private String getAdminApiUrl(){
        return "";
    }
}
