package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutgoingEmailFeedbackServiceTest {
    private static final String KEY = "0123456789abcdef0123456789abcdef";
    private static final String REQUEST = "150994dd-5e1f-40eb-a5a4-8a2dc28f1579";

    @Test void disabledOrMissingKeyRejectsWithoutStoreAccess() {
        TenantOutgoingEmailOutboxStore store = mock(TenantOutgoingEmailOutboxStore.class);
        assertThrows(IllegalStateException.class, () ->
                new OutgoingEmailFeedbackService(store, new EmailRecipientIdentity(KEY), false)
                        .recordVerifiedEvent(REQUEST, "provider:event-1", "DELIVERED"));
        assertThrows(IllegalStateException.class, () ->
                new OutgoingEmailFeedbackService(store, new EmailRecipientIdentity(""), true));
        verifyNoInteractions(store);
    }

    @Test void rejectsUnsupportedAndMalformedEventsBeforePersistence() {
        TenantOutgoingEmailOutboxStore store = mock(TenantOutgoingEmailOutboxStore.class);
        OutgoingEmailFeedbackService service = new OutgoingEmailFeedbackService(
                store, new EmailRecipientIdentity(KEY), true);
        assertThrows(IllegalArgumentException.class,
                () -> service.recordVerifiedEvent(REQUEST, "event-1", "QUEUED"));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordVerifiedEvent(REQUEST, "event-1", "DELIVERED\nBOUNCED"));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordVerifiedEvent("wrong", "event-1", "DELIVERED"));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordVerifiedEvent(REQUEST, "event whitespace", "BOUNCED"));
        verifyNoInteractions(store);
    }

    @Test void supportedEventsAreNormalisedAndDelegatedToTenantStore() {
        TenantOutgoingEmailOutboxStore store = mock(TenantOutgoingEmailOutboxStore.class);
        when(store.applyVerifiedFeedback(REQUEST, "provider:event-1", "BOUNCED")).thenReturn(true);
        OutgoingEmailFeedbackService service = new OutgoingEmailFeedbackService(
                store, new EmailRecipientIdentity(KEY), true);
        assertTrue(service.recordVerifiedEvent(REQUEST, "provider:event-1", " bounced "));
        verify(store).applyVerifiedFeedback(REQUEST, "provider:event-1", "BOUNCED");
    }
}
