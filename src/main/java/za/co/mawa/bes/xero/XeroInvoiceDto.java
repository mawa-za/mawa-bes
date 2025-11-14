package za.co.mawa.bes.xero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class XeroInvoiceDto {

    private String invoiceID;
    private String invoiceNumber;
    private String reference;
    private String status;
    private String type;
    private LocalDate date;
    private LocalDate dueDate;

    private Contact contact;
    private List<LineItem> lineItems;
    private BigDecimal subTotal;
    private BigDecimal totalTax;
    private BigDecimal total;

    // --- nested DTOs ---
    public static class Contact {
        private String name;
        // add email, addresses, etc as needed
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class LineItem {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitAmount;
        private BigDecimal lineAmount;
        private String accountCode;

        // getters/setters...
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

        public BigDecimal getUnitAmount() { return unitAmount; }
        public void setUnitAmount(BigDecimal unitAmount) { this.unitAmount = unitAmount; }

        public BigDecimal getLineAmount() { return lineAmount; }
        public void setLineAmount(BigDecimal lineAmount) { this.lineAmount = lineAmount; }

        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    }

    // getters/setters...

    public String getInvoiceID() { return invoiceID; }
    public void setInvoiceID(String invoiceID) { this.invoiceID = invoiceID; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Contact getContact() { return contact; }
    public void setContact(Contact contact) { this.contact = contact; }

    public List<LineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<LineItem> lineItems) { this.lineItems = lineItems; }

    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }

    public BigDecimal getTotalTax() { return totalTax; }
    public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}

