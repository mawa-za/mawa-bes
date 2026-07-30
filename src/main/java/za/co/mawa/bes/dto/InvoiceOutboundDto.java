package za.co.mawa.bes.dto;

import java.time.LocalDate;
import java.util.List;

public class InvoiceOutboundDto {
    private String id;
    private String invoiceNo;
    private String partnerId;
    private String partnerName;
    private String sourceType;
    private String sourceId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String status;
    private Integer subtotalCents;
    private Integer taxCents;
    private Integer discountCents;
    private Integer totalCents;
    private Integer paidCents;
    private Integer creditedCents;
    private Integer balanceCents;
    private String externalRef;
    private String notes;
    private String currency;
    private String xeroInvoiceId;
    private String xeroInvoiceNo;
    private String integrationStatus;
    private String integrationError;

    private List<InvoiceLineDto> lines;

    public static class InvoiceLineDto {
        private String productId;
        private String description;
        private Integer quantity;
        private Boolean showAmount;
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

        public Boolean getShowAmount() {
            return showAmount;
        }

        public void setShowAmount(Boolean showAmount) {
            this.showAmount = showAmount;
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

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public Integer getPaidCents() {
        return paidCents;
    }

    public void setPaidCents(Integer paidCents) {
        this.paidCents = paidCents;
    }

    public Integer getCreditedCents() {
        return creditedCents;
    }

    public void setCreditedCents(Integer creditedCents) {
        this.creditedCents = creditedCents;
    }

    public Integer getBalanceCents() {
        return balanceCents;
    }

    public void setBalanceCents(Integer balanceCents) {
        this.balanceCents = balanceCents;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getXeroInvoiceId() {
        return xeroInvoiceId;
    }

    public void setXeroInvoiceId(String xeroInvoiceId) {
        this.xeroInvoiceId = xeroInvoiceId;
    }

    public String getXeroInvoiceNo() {
        return xeroInvoiceNo;
    }

    public void setXeroInvoiceNo(String xeroInvoiceNo) {
        this.xeroInvoiceNo = xeroInvoiceNo;
    }

    public String getIntegrationStatus() {
        return integrationStatus;
    }

    public void setIntegrationStatus(String integrationStatus) {
        this.integrationStatus = integrationStatus;
    }

    public String getIntegrationError() {
        return integrationError;
    }

    public void setIntegrationError(String integrationError) {
        this.integrationError = integrationError;
    }

    public List<InvoiceLineDto> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLineDto> lines) {
        this.lines = lines;
    }
}