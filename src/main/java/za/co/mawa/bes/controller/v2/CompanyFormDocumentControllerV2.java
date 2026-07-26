package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.service.v2.CompanyFormDocumentService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/v2/company-forms")
@RequiredArgsConstructor
public class CompanyFormDocumentControllerV2 {
    private final CompanyFormDocumentService service;
    private final UserAccessService userAccessService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam(defaultValue = "true") boolean activeOnly) {
        if (!activeOnly) {
            assertSystemAdministrator();
        }
        return ResponseEntity.ok(service.list(activeOnly));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> upload(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) throws Exception {
        assertSystemAdministrator();
        return ResponseEntity.ok(service.upload(request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        assertSystemAdministrator();
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    private void assertSystemAdministrator() {
        if (!userAccessService.isProtectedAdministrator()) {
            throw new SecurityException("Only a system administrator can publish or unpublish company forms");
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) throws Exception {
        if (!service.isActive(id) && !userAccessService.isProtectedAdministrator()) {
            throw new SecurityException("This company form is not currently published");
        }
        CompanyFormDocumentService.Download download = service.download(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(download.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(download.bytes());
    }
}
