package za.co.mawa.bes.dto.v2.purple;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class PurpleDtos {
    private PurpleDtos() {}

    @Getter @Setter @NoArgsConstructor
    public static class ProviderEnrolmentRequest {
        private String publicSlug;
        private String displayName;
        private String description;
        private String logoUrl;
        private String contactEmail;
        private String contactNumber;
        private Boolean bookingEnabled;
        private Boolean serviceRequestEnabled;
        private Boolean active;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ServiceEnrolmentRequest {
        private String productId;
        private String displayName;
        private String description;
        private Boolean bookingEnabled;
        private Boolean serviceRequestEnabled;
        private Integer durationMinutes;
        private Integer slotIntervalMinutes;
        private Integer bufferBeforeMinutes;
        private Integer bufferAfterMinutes;
        private String location;
        private Integer displayOrder;
        private Boolean active;
    }

    @Getter @Setter @NoArgsConstructor
    public static class AvailabilityRuleRequest {
        private String id;
        private String serviceEnrolmentId;
        private String employeePartnerId;
        private Integer dayOfWeek;
        private String startTime;
        private String endTime;
        private LocalDate validFrom;
        private LocalDate validTo;
        private String location;
        private Boolean active;
    }

    @Getter @Setter @NoArgsConstructor
    public static class AvailabilityRequest {
        private String serviceId;
        private LocalDate fromDate;
        private LocalDate toDate;
    }

    @Getter @Setter @NoArgsConstructor
    public static class CustomerRequest {
        private String purpleCustomerId;
        private String email;
        private String cellphone;
        private String displayName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class BookingRequest extends CustomerRequest {
        private String serviceId;
        private String employeePartnerId;
        private LocalDate appointmentDate;
        private String startTime;
        private String notes;
        private String location;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ServiceRequestCreate extends CustomerRequest {
        private String serviceId;
        private String summary;
        private String description;
        private String category;
        private String priority;
        private Map<String, Object> additionalDetails;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ProviderConfigurationResponse {
        private Map<String, Object> provider;
        private List<Map<String, Object>> services;
        private List<Map<String, Object>> availabilityRules;
    }
}
