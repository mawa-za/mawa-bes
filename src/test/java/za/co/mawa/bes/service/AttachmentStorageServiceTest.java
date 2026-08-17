package za.co.mawa.bes.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.context.TenantContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttachmentStorageServiceTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void attachmentFolderUsesTenantIdEvenWhenTenantUrlIsPresent() {
        TenantContext.setCurrentTenant("tenant-123");
        TenantContext.setCurrentTenantURL("tenant.example.mawa.co.za");

        AttachmentStorageService service = new AttachmentStorageService("bucket", "attachments", "GCP");

        assertEquals("tenant-123", service.resolveTenantPathScope());
    }

    @Test
    void attachmentFolderRequiresTenantIdInsteadOfFallingBackToTenantUrl() {
        TenantContext.setCurrentTenantURL("tenant.example.mawa.co.za");
        AttachmentStorageService service = new AttachmentStorageService("bucket", "attachments", "GCP");

        assertThrows(IllegalStateException.class, service::resolveTenantPathScope);
    }
}
