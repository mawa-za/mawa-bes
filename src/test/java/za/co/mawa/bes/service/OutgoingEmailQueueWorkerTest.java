package za.co.mawa.bes.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.v2.BackgroundExecutionContextService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutgoingEmailQueueWorkerTest {
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void featureIsDisabledByDefaultAndDoesNotLookUpTenants() {
        TenantAdminService tenants = mock(TenantAdminService.class);
        var worker = new OutgoingEmailQueueWorker(tenants,
                mock(BackgroundExecutionContextService.class),
                mock(TenantOutgoingEmailOutboxStore.class),
                mock(SharedOutgoingEmailService.class), false, 5);
        worker.dispatch();
        verifyNoInteractions(tenants);
    }

    @Test void retriesTransientSmtpErrorAndClearsTenantContext() {
        TenantDto tenant = mock(TenantDto.class);
        when(tenant.getId()).thenReturn("qa_tenant");
        TenantAdminService tenants = mock(TenantAdminService.class);
        when(tenants.getAll()).thenReturn(List.of(tenant));
        var store = mock(TenantOutgoingEmailOutboxStore.class);
        var background = mock(BackgroundExecutionContextService.class);
        var mail = mock(SharedOutgoingEmailService.class);
        var lease = new TenantOutgoingEmailOutboxStore.Lease("req1", "invoice", new byte[]{1, 2}, 1, "token1");
        when(store.claimNext(5)).thenReturn(Optional.of(lease), Optional.empty());
        when(mail.sendPrepared(any(byte[].class), eq("invoice"), eq("req1")))
                .thenThrow(new MailSendException("temporary connection problem"));
        var worker = new OutgoingEmailQueueWorker(tenants, background, store, mail, true, 5);
        worker.dispatch();
        verify(store).failed(eq(lease), eq(false), eq(5), eq("MailSendException"));
        verify(background).clear();
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test void successfulSmtpAcceptanceRecordsMessageIdWithoutClaimingDelivery() {
        TenantDto tenant = mock(TenantDto.class);
        when(tenant.getId()).thenReturn("qa_tenant");
        TenantAdminService tenants = mock(TenantAdminService.class);
        when(tenants.getAll()).thenReturn(List.of(tenant));
        var store = mock(TenantOutgoingEmailOutboxStore.class);
        var mail = mock(SharedOutgoingEmailService.class);
        var lease = new TenantOutgoingEmailOutboxStore.Lease("req1", "invoice", new byte[]{1, 2}, 1, "token1");
        when(store.claimNext(5)).thenReturn(Optional.of(lease), Optional.empty());
        when(mail.sendPrepared(any(byte[].class), eq("invoice"), eq("req1"))).thenReturn("<mime1@example.com>");
        var worker = new OutgoingEmailQueueWorker(tenants,
                mock(BackgroundExecutionContextService.class), store, mail, true, 5);
        worker.dispatch();
        verify(store).accepted(lease, "<mime1@example.com>");
    }
}
