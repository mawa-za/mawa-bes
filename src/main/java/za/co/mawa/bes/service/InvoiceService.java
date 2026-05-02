package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.repository.InvoiceLineRepository;
import za.co.mawa.bes.repository.InvoicePaymentRepository;
import za.co.mawa.bes.repository.InvoiceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceLineRepository invoiceLineRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    public InvoiceEntity createInvoice(InvoiceEntity invoice) {
        invoice.setId(UUID.randomUUID().toString());
        invoice.getLines().forEach(line -> {
            line.setId(UUID.randomUUID().toString());
            line.setInvoice(invoice); // Ensure proper linkage
        });
        invoice.getPayments().forEach(payment -> {
            payment.setId(UUID.randomUUID().toString());
            payment.setInvoice(invoice); // Ensure proper linkage
        });
        return invoiceRepository.save(invoice);
    }

    public Optional<InvoiceEntity> getInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    public List<InvoiceLineEntity> getInvoiceLines(String invoiceId) {
        return invoiceLineRepository.findByInvoiceId(invoiceId);
    }

    public List<InvoicePaymentEntity> getInvoicePayments(String invoiceId) {
        return invoicePaymentRepository.findByInvoiceId(invoiceId);
    }

    public void deleteInvoice(String invoiceId) {
        invoiceRepository.deleteById(invoiceId);
    }
    public List<InvoiceEntity> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<InvoiceEntity> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status);
    }

    public List<InvoiceEntity> getInvoicesByPartnerId(String partnerId) {
        return invoiceRepository.findByPartnerId(partnerId);
    }

    public List<InvoiceEntity> getInvoicesByDate(String invoiceDate) {
        LocalDate date = LocalDate.parse(invoiceDate);
        return invoiceRepository.findByInvoiceDate(date);
    }
}