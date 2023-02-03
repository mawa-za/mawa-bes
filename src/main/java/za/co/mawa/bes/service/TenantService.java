package za.co.mawa.bes.service;

import com.nimbusds.jose.shaded.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.JwtRequest;
import za.co.mawa.bes.dao.TenantDao;
import za.co.mawa.bes.dto.TenantDto;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class TenantService implements TenantDao {
    @Autowired
    SettingService settingService;

    Gson gson = new Gson();


    @Value("${mawa.admin.api.url}")
    private String ADMIN_API_URL;
    @Value("${mawa.admin.api.password}")
    private String ADMIN_USERNAME;
    @Value("${mawa.admin.api.password}")
    private String ADMIN_PASSWORD;

    @Override
    public String getAdminToken() {
        JwtRequest tokenRequest = new JwtRequest();
        tokenRequest.setUsername(ADMIN_USERNAME);
        tokenRequest.setId(ADMIN_USERNAME);
        tokenRequest.setPassword(ADMIN_PASSWORD);
        String token = "";
        try {
            URL url = new URL(ADMIN_API_URL + "/authenticate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-TenantID", "default");
            conn.setDoOutput(true);
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
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return token;
    }

    @Override
    public List<TenantDto> getAll() {
        List<TenantDto> tenants = new ArrayList<>();
        String jsonString = "";
        try {
            String token = getAdminToken();
            URL url = new URL(ADMIN_API_URL + "/tenant");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "*/*");
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
            List<JSONObject> list = new ArrayList();
            for (int i = 0; i < array.length(); i++) {
                TenantDto tenantDto = gson.fromJson(String.valueOf(array.getJSONObject(i)), TenantDto.class);
                tenants.add(tenantDto);
            }

            return tenants;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Properties getTenantProperties(String tenant) {
        Properties props = new Properties();
        String jsonString = "";
        try {
            String token = getAdminToken();
            URL url = new URL(ADMIN_API_URL + "/tenant/" + tenant + "/property");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "*/*");
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
            JSONObject jsonObject = new JSONObject(jsonString);

            props = Property.toProperties(jsonObject);
            return props;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getAdminApiUrl() {
        return "";
    }
}
