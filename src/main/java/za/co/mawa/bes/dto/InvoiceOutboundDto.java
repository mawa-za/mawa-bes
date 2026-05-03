package za.co.mawa.bes.dto;

import java.time.LocalDate;
import java.util.List;

public class InvoiceOutboundDto {
    private String id;
    private String partnerId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String status;
    private Integer subtotalCents;
    private Integer taxCents;
    private Integer discountCents;
    private Integer totalCents;
    private String currency;

    private List<InvoiceLineDto> lines;

    public static class InvoiceLineDto {
        private String productId;
        private String description;
        private Integer quantity;
        private Integer unitPriceCents;
        private Integer discountCents;
        private Integer taxCents;
        private Integer subtotalCents;
        private Integer totalCents;

        // Getters and Setters
        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getUnitPriceCents() {
            return unitPriceCents;
        }

        public void setUnitPriceCents(Integer unitPriceCents) {
            this.unitPriceCents = unitPriceCents;
        }

        public Integer getDiscountCents() {
            return discountCents;
        }

        public void setDiscountCents(Integer discountCents) {
            this.discountCents = discountCents;
        }

        public Integer getTaxCents() {
            return taxCents;
        }

        public void setTaxCents(Integer taxCents) {
            this.taxCents = taxCents;
        }

        public Integer getSubtotalCents() {
            return subtotalCents;
        }

        public void setSubtotalCents(Integer subtotalCents) {
            this.subtotalCents = subtotalCents;
        }

        public Integer getTotalCents() {
            return totalCents;
        }

        public void setTotalCents(Integer totalCents) {
            this.totalCents = totalCents;
        }
    }

    // Getters and Setters for the main DTO class
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSubtotalCents() {
        return subtotalCents;
    }

    public void setSubtotalCents(Integer subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public Integer getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(Integer taxCents) {
        this.taxCents = taxCents;
    }

    public Integer getDiscountCents() {
        return discountCents;
    }

    public void setDiscountCents(Integer discountCents) {
        this.discountCents = discountCents;
    }

    public Integer getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(Integer totalCents) {
        this.totalCents = totalCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<InvoiceLineDto> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLineDto> lines) {
        this.lines = lines;
    }
}