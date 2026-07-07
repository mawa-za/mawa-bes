package za.co.mawa.bes.controller.v2;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;
import za.co.mawa.bes.service.CompanyLogoService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/v2/company-logo")
public class CompanyLogoControllerV2 {
    private final CompanyLogoService companyLogoService;

    public CompanyLogoControllerV2(CompanyLogoService companyLogoService) {
        this.companyLogoService = companyLogoService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> metadata() {
        return ResponseEntity.ok(companyLogoService.metadata());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
                                                       @RequestHeader(value = "X-User-Id", required = false) String currentUser) {
        try {
            companyLogoService.upload(file, currentUser);
            return ResponseEntity.ok(companyLogoService.metadata());
        } catch (Exception e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", e.getMessage() == null ? e.toString() : e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/content")
    public ResponseEntity<ByteArrayResource> content() {
        CompanyLogoEntity logo = companyLogoService.getActiveLogo().orElseThrow(() -> new IllegalArgumentException("Company logo not loaded"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + logo.getFileName())
                .contentType(MediaType.parseMediaType(logo.getContentType()))
                .body(new ByteArrayResource(logo.getContent()));
    }
}
