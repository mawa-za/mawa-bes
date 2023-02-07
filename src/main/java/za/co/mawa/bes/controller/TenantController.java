package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.TenantPropertyDto;
import za.co.mawa.bes.service.EncryptionService;
import za.co.mawa.bes.service.TenantService;

import java.util.List;
import java.util.Properties;

@RestController
@CrossOrigin
public class TenantController {
    @Autowired
    TenantService tenantService;

    @Autowired
    EncryptionService encryptionService;
    @Value("${jwt.secret}")
    private String secret;
    Gson gson = new Gson();


    @RequestMapping(value = "/tenant", method = RequestMethod.POST)
    public ResponseEntity<?> postTenant(@RequestHeader HttpHeaders headers, @RequestBody TenantDto tenantDto) throws Exception {
        try {
            tenantDto = tenantService.create(tenantDto);
            String password = encryptionService.encrypt(tenantDto.getDatabase_password(), secret);
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "jwt.secret", secret));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "hibernate.connection.password", password));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "hibernate.connection.url", tenantDto.getDatabase_url()));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "hibernate.connection.username", tenantDto.getDatabase_username()));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "hibernate.dialect", "org.hibernate.dialect.MySQLDialect"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "hibernate.default_schema", "mawa"));

            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "flyway.url", tenantDto.getDatabase_url()));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "flyway.user", tenantDto.getDatabase_username()));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "flyway.password", password));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.flyway.baseline-on-migrate", "true"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.flyway.enabled", "false"));

            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.datasource.url", tenantDto.getDatabase_url()));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.datasource.password", password));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.datasource.username", tenantDto.getDatabase_username()));

            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.jpa.generate-ddl", "true"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.jpa.hibernate.ddl-auto", "update"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.MySQLDialect"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect"));
            tenantService.addProperty(new TenantPropertyDto(tenantDto.getId(), "spring.jpa.show-sql", "true"));

            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/tenant", method = RequestMethod.GET)
    public ResponseEntity<?> getTenants(@RequestHeader HttpHeaders headers) throws Exception {
        List<TenantDto> tenantDtoList = tenantService.getAll();
        return ResponseEntity.ok(gson.toJson(tenantDtoList));
    }

    @RequestMapping(value = "/tenant/{tenant}/property", method = RequestMethod.GET)
    public ResponseEntity<?> getTenantProperties(@RequestHeader HttpHeaders headers, @PathVariable String tenant) throws Exception {
        Properties properties = tenantService.getTenantProperties(tenant);
        return ResponseEntity.ok(gson.toJson(tenantService.getTenantProperties(tenant)));
    }

    @RequestMapping(value = "/tenant/{tenant}/property", method = RequestMethod.POST)
    public ResponseEntity<?> postTenantProperty(@RequestHeader HttpHeaders headers, @PathVariable String tenant, @RequestBody TenantPropertyDto tenantPropertyDto) throws Exception {
        tenantPropertyDto.setTenant(tenant);
        tenantService.addProperty(tenantPropertyDto);
        return ResponseEntity.ok().build();
    }
}
