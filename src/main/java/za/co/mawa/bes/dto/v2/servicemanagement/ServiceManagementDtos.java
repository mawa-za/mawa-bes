package za.co.mawa.bes.dto.v2.servicemanagement;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ServiceManagementDtos {
    private ServiceManagementDtos() {}

    @Data
    public static class LocationRequest {
        private String id;
        private String customerPartnerId;
        private String name;
        private String addressLine1;
        private String addressLine2;
        private String suburb;
        private String city;
        private String province;
        private String postalCode;
        private String contactName;
        private String contactNumber;
        private String contactEmail;
        private String accessInstructions;
        private String serviceNotes;
        private Double latitude;
        private Double longitude;
        private Boolean active;
    }

    @Data
    public static class ResourceRequest {
        private String id;
        private String name;
        private String resourceType;
        private String employeePartnerId;
        private Integer capacity;
        private String location;
        private Boolean active;
        private String notes;
    }

    @Data
    public static class ResourceRequirementRequest {
        private String id;
        private String productId;
        private String resourceType;
        private String resourceId;
        private Integer quantity;
        private Boolean mandatory;
    }

    @Data
    public static class ContractLineRequest {
        private String id;
        private String productId;
        private String description;
        private Double quantity;
        private Long unitPriceCents;
        private Long discountCents;
        private Long taxCents;
        private Boolean active;
    }

    @Data
    public static class ContractScheduleRequest {
        private String id;
        private String productId;
        private String frequency;
        private Integer intervalCount;
        private Integer dayOfWeek;
        private Integer dayOfMonth;
        private LocalTime preferredStartTime;
        private Integer durationMinutes;
        private Integer generationHorizonDays;
        private LocalDate nextGenerationDate;
        private Boolean active;
    }

    @Data
    public static class ContractRequest {
        private String id;
        private String customerPartnerId;
        private String billingPartnerId;
        private String serviceLocationId;
        private String quotationId;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private String billingFrequency;
        private String billingTiming;
        private String billingMode;
        private String currency;
        private String notes;
        private List<ContractLineRequest> lines;
        private List<ContractScheduleRequest> schedules;
    }

    @Data
    public static class RequestMetadataRequest {
        private String serviceRequestId;
        private String productId;
        private String serviceLocationId;
        private String sourceChannel;
        private String externalRequestId;
        private LocalDate preferredDate;
        private LocalTime preferredStartTime;
        private Boolean recurringRequested;
        private String recurrenceFrequency;
        private Integer recurrenceInterval;
    }

    @Data
    public static class AvailabilityRequest {
        private String productId;
        private LocalDate date;
        private LocalTime startTime;
        private Integer durationMinutes;
    }
}
