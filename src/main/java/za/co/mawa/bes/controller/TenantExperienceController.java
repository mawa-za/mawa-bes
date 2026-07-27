package za.co.mawa.bes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.TenantService;

import java.util.Properties;

@RestController
@CrossOrigin
public class TenantExperienceController {
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping(value = "/tenant-experience", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTenantExperience() {
        try {
            String tenant = TenantContext.getCurrentTenant();
            Properties properties = tenantService.getTenantProperties(tenant);
            String configuredExperience = properties.getProperty("TENANT.WORKCENTER_EXPERIENCE");
            if (StringUtils.hasText(configuredExperience)) {
                JsonNode experience = objectMapper.readTree(configuredExperience);
                if (experience != null && experience.isObject()) {
                    return ResponseEntity.ok(experience);
                }
            }

            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("tenantId", tenant);
            fallback.put("primaryIndustryCode",
                    properties.getProperty("TENANT.PRIMARY_INDUSTRY", "GENERAL_CUSTOM"));
            fallback.put("primaryIndustryName", "General / Custom");
            ArrayNode additional = fallback.putArray("additionalIndustries");
            String configuredAdditional = properties.getProperty("TENANT.ADDITIONAL_INDUSTRIES", "");
            if (StringUtils.hasText(configuredAdditional)) {
                for (String industry : configuredAdditional.split(",")) {
                    if (StringUtils.hasText(industry)) {
                        ObjectNode item = additional.addObject();
                        item.put("code", industry.trim());
                        item.put("name", industry.trim().replace('_', ' '));
                    }
                }
            }
            fallback.putArray("sections");
            return ResponseEntity.ok(fallback);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
}
