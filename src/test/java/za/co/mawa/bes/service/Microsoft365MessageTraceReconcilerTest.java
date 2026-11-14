package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class Microsoft365MessageTraceReconcilerTest {
    private static final String REQUEST = "01234567-1234-1234-1234-123456789abc";
    private static final String TRACE = "22334455-1234-1234-1234-123456789abc";
    private static final String MAILBOX = "noreply@mawa.co.za";
    private static final String MSG = "<message-001@mawa.co.za>";
    private static final String RECIPIENT = "test@example.org";

    private final TenantOutgoingEmailOutboxStore store = mock(TenantOutgoingEmailOutboxStore.class);
    private final OutgoingEmailFeedbackService feedback = mock(OutgoingEmailFeedbackService.class);
    private final Microsoft365MessageTraceClient client = mock(Microsoft365MessageTraceClient.class);
    private final EmailRecipientIdentity identity = new EmailRecipientIdentity("32-characters-long-qa-hmac-secret-key");

    private TenantOutgoingEmailOutboxStore.TraceCandidate candidate(Instant at) {
        return new TenantOutgoingEmailOutboxStore.TraceCandidate(REQUEST, MSG,
                identity.fingerprint(RECIPIENT), at);
    }
    private Microsoft365MessageTraceReconciler service() {
        when(client.approvedSender()).thenReturn(MAILBOX);
        return new Microsoft365MessageTraceReconciler(null, null, store, client,
                identity, feedback, true, true, 5);
    }

    @Test void exactDeliveredTraceBecomesVerifiedFeedbackOnlyOnce() {
        Instant at = Instant.now().minusSeconds(1800);
        when(client.findByMessageId(MSG, at)).thenReturn(List.of(new Microsoft365MessageTraceClient.Trace(
                TRACE, MSG, MAILBOX, RECIPIENT, "delivered", at.plusSeconds(40))));
        service().reconcileOne(candidate(at));
        verify(store).recordTraceObservation(REQUEST, "DELIVERED", TRACE);
        verify(feedback).recordVerifiedEvent(eq(REQUEST),
                eq(Microsoft365MessageTraceReconciler.eventId(REQUEST, TRACE)), eq("DELIVERED"));
    }

    @Test void wrongRecipientCannotDeliverAcrossTenants() {
        Instant at = Instant.now().minusSeconds(1800);
        when(client.findByMessageId(MSG, at)).thenReturn(List.of(new Microsoft365MessageTraceClient.Trace(
                TRACE, MSG, MAILBOX, "other-tenant@example.org", "delivered", at.plusSeconds(40))));
        service().reconcileOne(candidate(at));
        verify(store).recordTraceObservation(REQUEST, "NOT_FOUND", null);
        verifyNoInteractions(feedback);
    }

    @Test void wrongSenderOrDuplicateMatchesNeverConfirmDelivery() {
        Instant at = Instant.now().minusSeconds(1800);
        Microsoft365MessageTraceClient.Trace forged = new Microsoft365MessageTraceClient.Trace(
                TRACE, MSG, "attacker@example.com", RECIPIENT, "delivered", at.plusSeconds(40));
        when(client.findByMessageId(MSG, at)).thenReturn(List.of(forged));
        service().reconcileOne(candidate(at));
        verify(store).recordTraceObservation(REQUEST, "NOT_FOUND", null);
        verifyNoInteractions(feedback);
    }

    @Test void ambiguousAndMalformedRecipientNeverConfirmDelivery() {
        Instant at = Instant.now().minusSeconds(1800);
        Microsoft365MessageTraceClient.Trace first = new Microsoft365MessageTraceClient.Trace(
                TRACE, MSG, MAILBOX, RECIPIENT, "delivered", at.plusSeconds(40));
        Microsoft365MessageTraceClient.Trace second = new Microsoft365MessageTraceClient.Trace(
                "32334455-1234-1234-1234-123456789abc", MSG, MAILBOX,
                RECIPIENT, "delivered", at.plusSeconds(41));
        when(client.findByMessageId(MSG, at)).thenReturn(List.of(first, second));
        service().reconcileOne(candidate(at));
        verify(store).recordTraceObservation(REQUEST, "AMBIGUOUS", null);
        verifyNoInteractions(feedback);

        clearInvocations(store, feedback);
        when(client.findByMessageId(MSG, at)).thenReturn(List.of(
                new Microsoft365MessageTraceClient.Trace(TRACE, MSG, MAILBOX,
                        "invalid recipient address", "delivered", at.plusSeconds(40))));
        service().reconcileOne(candidate(at));
        verify(store).recordTraceObservation(REQUEST, "NOT_FOUND", null);
        verifyNoInteractions(feedback);
    }

    @Test void failedTraceIsDiagnosticButDoesNotSuppressRecipient() {
        Instant at = Instant.now().minusSeconds(1800);
        when(client.findByMessageId(MSG, at)).thenReturn(List.of(new Microsoft365MessageTraceClient.Trace(
                TRACE, MSG, MAILBOX, RECIPIENT, "failed", at.plusSeconds(40))));
        service().reconcileOne(candidate(at));
        verify(store).recordTraceObservation(REQUEST, "FAILED", TRACE);
        verifyNoInteractions(feedback);
    }

    @Test void traceEventIdentityIsStableAndDoesNotLeakRecipient() {
        String eventId = Microsoft365MessageTraceReconciler.eventId(REQUEST, TRACE);
        assertEquals(eventId, Microsoft365MessageTraceReconciler.eventId(REQUEST, TRACE));
        assertTrue(eventId.startsWith("m365-trace-"));
        assertFalse(eventId.contains(RECIPIENT));
    }
}
