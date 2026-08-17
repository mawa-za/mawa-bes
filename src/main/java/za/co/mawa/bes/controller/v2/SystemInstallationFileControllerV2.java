package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.v2.SystemInstallationFileDtos.DownloadResponse;
import za.co.mawa.bes.dto.v2.SystemInstallationFileDtos.Response;
import za.co.mawa.bes.dto.v2.SystemInstallationFileDtos.UploadRequest;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.service.v2.SystemInstallationFileService;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/v2/system-installation-files")
@RequiredArgsConstructor
public class SystemInstallationFileControllerV2 {
    private final SystemInstallationFileService service;
    private final UserAccessService userAccessService;

    @GetMapping
    public List<Response> list() {
        return service.list();
    }

    @PostMapping
    public Response upload(@RequestBody UploadRequest request) {
        assertCanManageInstallationFiles();
        return service.upload(request);
    }

    @GetMapping("/{id}/download")
    public DownloadResponse download(@PathVariable String id) throws Exception {
        return service.download(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        assertCanManageInstallationFiles();
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void assertCanManageInstallationFiles() {
        if (!userAccessService.isProtectedAdministrator()) {
            throw new SecurityException(
                    "Only a protected tenant administrator can manage installation files");
        }
    }
}
