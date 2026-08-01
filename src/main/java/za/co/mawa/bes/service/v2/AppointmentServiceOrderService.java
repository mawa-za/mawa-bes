package za.co.mawa.bes.service.v2;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderRequest;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderResponse;
import za.co.mawa.bes.entity.InvoiceEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * Compatibility facade for clients compiled against the original appointment-specific service.
 * New code must use {@link ServiceOrderService}.
 */
@Deprecated
@Service
public class AppointmentServiceOrderService {
    private final ServiceOrderService delegate;

    public AppointmentServiceOrderService(ServiceOrderService delegate) {
        this.delegate = delegate;
    }

    public ServiceOrderResponse createFromAppointment(String appointmentId, String currentUser) {
        return delegate.createFromAppointment(appointmentId, currentUser);
    }

    public ServiceOrderResponse get(String id) {
        return delegate.get(id);
    }

    public List<ServiceOrderResponse> search(
            String status, String customerId, String appointmentId, LocalDate fromDate, LocalDate toDate) {
        return delegate.search(status, customerId,
                appointmentId == null || appointmentId.isBlank() ? null : "APPOINTMENT",
                appointmentId, fromDate, toDate);
    }

    public ServiceOrderResponse update(String id, ServiceOrderRequest request, String currentUser) {
        return delegate.update(id, request, currentUser);
    }

    public InvoiceEntity createInvoice(String id, String currentUser) {
        return delegate.createInvoice(id, currentUser);
    }
}
