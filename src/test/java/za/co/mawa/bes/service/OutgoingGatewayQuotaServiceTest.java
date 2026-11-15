package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutgoingGatewayQuotaServiceTest {
    private static final String EVENT = "billing-invoice-123-issued";
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void disabledQuotaDoesNotTouchDatabase() {
        TenantContext.setCurrentTenant("qa_tenant");
        EntityManager em = mock(EntityManager.class);
        new OutgoingGatewayQuotaService(em, false, 50, 500, 100)
                .reserve("billing", "BILL-123", EVENT);
        verifyNoInteractions(em);
    }

    @Test void invalidTenantAndSourceAreRejected() {
        EntityManager em = mock(EntityManager.class);
        var quota = new OutgoingGatewayQuotaService(em, true, 50, 500, 100);
        assertThrows(SecurityException.class, () -> quota.reserve("billing", "BILL-123", EVENT));
        TenantContext.setCurrentTenant("qa_tenant");
        assertThrows(SecurityException.class, () -> quota.reserve("customer", "BILL-123", EVENT));
        assertThrows(IllegalArgumentException.class, () -> quota.reserve("billing", "../bad?", EVENT));
        verifyNoInteractions(em);
    }

    @Test void enforcesTenantDayLimitAndAllowsIdempotentReplay() throws Exception {
        TenantContext.setCurrentTenant("qa_tenant");
        EntityManager em = mock(EntityManager.class);
        Session session = mock(Session.class);
        Connection conn = mock(Connection.class);
        PreparedStatement init = mock(PreparedStatement.class), lock = mock(PreparedStatement.class);
        PreparedStatement existing = mock(PreparedStatement.class), charge = mock(PreparedStatement.class);
        ResultSet budgetRows = mock(ResultSet.class), dedupRows = mock(ResultSet.class);
        when(em.unwrap(Session.class)).thenReturn(session);
        when(conn.prepareStatement(contains("INSERT INTO outbound_email_gateway_daily_quota"))).thenReturn(init);
        when(conn.prepareStatement(contains("SELECT accepted_count"))).thenReturn(lock);
        when(conn.prepareStatement(contains("SELECT id FROM outbound_email_message"))).thenReturn(existing);
        when(conn.prepareStatement(contains("SET accepted_count=accepted_count+1"))).thenReturn(charge);
        when(lock.executeQuery()).thenReturn(budgetRows);
        when(existing.executeQuery()).thenReturn(dedupRows);
        when(budgetRows.next()).thenReturn(true);
        when(budgetRows.getInt(1)).thenReturn(2);
        when(charge.executeUpdate()).thenReturn(1);
        // Test the real Session#doWork body using a stub callback.
        doAnswer(call -> {
            org.hibernate.jdbc.Work work = call.getArgument(0);
            work.execute(conn);
            return null;
        }).when(session).doWork(any(org.hibernate.jdbc.Work.class));
        var quota = new OutgoingGatewayQuotaService(em, true, 50, 2, 100);
        assertThrows(OutgoingGatewayQuotaService.QuotaExceededException.class,
                () -> quota.reserve("billing", "BILL-123", EVENT));
        verify(charge, never()).executeUpdate();
        // At the limit, an existing identical event remains free.
        when(dedupRows.next()).thenReturn(true);
        assertDoesNotThrow(() -> quota.reserve("billing", "BILL-123", EVENT));
        verify(charge, never()).executeUpdate();
    }

    @Test void freshEventChargesOnlyOneSlot() throws Exception {
        TenantContext.setCurrentTenant("qa_tenant");
        EntityManager em = mock(EntityManager.class);
        Session session = mock(Session.class);
        Connection conn = mock(Connection.class);
        PreparedStatement init = mock(PreparedStatement.class), lock = mock(PreparedStatement.class);
        PreparedStatement existing = mock(PreparedStatement.class), charge = mock(PreparedStatement.class);
        ResultSet budgetRows = mock(ResultSet.class), dedupRows = mock(ResultSet.class);
        when(em.unwrap(Session.class)).thenReturn(session);
        when(conn.prepareStatement(contains("INSERT INTO outbound_email_gateway_daily_quota"))).thenReturn(init);
        when(conn.prepareStatement(contains("SELECT accepted_count"))).thenReturn(lock);
        when(conn.prepareStatement(contains("SELECT id FROM outbound_email_message"))).thenReturn(existing);
        when(conn.prepareStatement(contains("SET accepted_count=accepted_count+1"))).thenReturn(charge);
        when(lock.executeQuery()).thenReturn(budgetRows);
        when(existing.executeQuery()).thenReturn(dedupRows);
        when(budgetRows.next()).thenReturn(true);
        when(budgetRows.getInt(1)).thenReturn(0);
        when(dedupRows.next()).thenReturn(false);
        when(charge.executeUpdate()).thenReturn(1);
        doAnswer(call -> {
            org.hibernate.jdbc.Work work = call.getArgument(0);
            work.execute(conn);
            return null;
        }).when(session).doWork(any(org.hibernate.jdbc.Work.class));
        new OutgoingGatewayQuotaService(em, true, 50, 1, 100)
                .reserve("billing", "BILL-123", EVENT);
        verify(charge).setString(1, "billing");
        verify(charge).setInt(2, 1);
        verify(charge, times(1)).executeUpdate();
        verify(existing).setString(2, QueuedOutgoingEmailService.hash("billing\nBILL-123\n" + EVENT));
    }

    @Test void invalidLimitsRejectedOnStartup() {
        assertThrows(IllegalArgumentException.class, () ->
                new OutgoingGatewayQuotaService(mock(EntityManager.class), false, 50, 0, 100));
    }
}
