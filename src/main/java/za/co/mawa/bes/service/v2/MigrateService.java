package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.transaction.TransactionPartnerEntity;
import za.co.mawa.bes.entity.transaction.TransactionPartnerPKEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.TransactionPartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.PremiumService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class MigrateService {
    private static final Set<String> DEPENDENT_PARTNER_FUNCTIONS = Set.of("DEPENDENT", "DEPENDANT");
    public static final String LEGACY_MEMBERSHIP_MIGRATION_JOB = "LEGACY_MEMBERSHIP_MIGRATION";
    private static final String LEGACY_MIGRATION_GROUP = "LEGACY-MIGRATION";
    private static final String ENABLED = "ENABLED";
    private static final String INTERVAL_MINUTES = "INTERVAL-MINUTES";
    private static final String LAST_RUN_AT = "LAST-RUN-AT";

    @Autowired
    TransactionService transactionService;
    @Autowired
    @Qualifier("MembershipServiceV2")
    MembershipService membershipService;
    @Autowired
    MembershipDependentService membershipDependentService;
    @Autowired
    MembershipRepository membershipRepository;
    @Autowired
    MembershipDependentRepository membershipDependentRepository;
    @Autowired
    TransactionPartnerRepository transactionPartnerRepository;
    @Autowired
    PartnerRepository partnerRepository;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    ProductService productService;
    @Autowired
    SettingService settingService;
    @Autowired
    MembershipPlanRepository membershipPlanRepository;
    @Autowired
    MembershipPlanService membershipPlanService;
    @Autowired
    PremiumService premiumService;

    public void migrateMembershipPlans() {
        migrateMembershipPlans(null);
    }

    private void migrateMembershipPlans(MembershipMigrationResult.TenantResult tenantResult) {
        try {
            List<ProductDto> productDtoList = productService.query("MEMBERSHIP", "%");
            for (ProductDto productDto : productDtoList) {
                try {
                    if (productDto == null || isBlank(productDto.getId())) {
                        incrementPlanFailure(tenantResult, "Skipped membership plan with missing old product id");
                        continue;
                    }

                    MembershipPlanEntity membershipPlanEntity = membershipPlanRepository.findByOldId(productDto.getId()).orElse(null);
                    if (membershipPlanEntity == null) {
                        membershipPlanEntity = new MembershipPlanEntity();
                        membershipPlanEntity.setPlanCode(firstNonBlank(productDto.getCode(), productDto.getId()));
                        membershipPlanEntity.setOldId(productDto.getId());
                        membershipPlanEntity.setDescription(productDto.getDescription());
                        membershipPlanEntity.setName(firstNonBlank(productDto.getDescription(), productDto.getCode(), productDto.getId()));
                        membershipPlanEntity.setCurrency("ZAR");
                        membershipPlanEntity.setMaxDependents(15);
                        membershipPlanEntity.setActive(true);
                        membershipPlanEntity.setPremiumCents(19000L);
                        membershipPlanService.createPlan(membershipPlanEntity);
                        if (tenantResult != null) {
                            tenantResult.setPlansCreated(tenantResult.getPlansCreated() + 1);
                        }
                    } else if (tenantResult != null) {
                        tenantResult.setPlansAlreadyExisting(tenantResult.getPlansAlreadyExisting() + 1);
                    }
                } catch (Exception e) {
                    incrementPlanFailure(tenantResult, "Plan " + safe(productDto == null ? null : productDto.getId()) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            incrementPlanFailure(tenantResult, "Unable to load old membership products: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${mawa.scheduler.dispatcher-delay-ms:30000}")
    public void migrateMembershipsScheduled() {
        if (!isScheduledMigrationEnabled() || !isScheduledMigrationDue()) {
            return;
        }
        markScheduledMigrationRun();
        migrateMemberships();
    }

    public boolean isScheduledMigrationEnabled() {
        String enabled = settingService.getSetting(ENABLED, LEGACY_MIGRATION_GROUP);
        return "true".equalsIgnoreCase(enabled) || "1".equals(enabled) || "Y".equalsIgnoreCase(enabled);
    }

    public int getScheduledMigrationIntervalMinutes() {
        String value = settingService.getSetting(INTERVAL_MINUTES, LEGACY_MIGRATION_GROUP);
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(5, Math.min(parsed, 10080));
        } catch (Exception ignored) {
            return 1440;
        }
    }

    public LocalDateTime getScheduledMigrationLastRunAt() {
        String value = settingService.getSetting(LAST_RUN_AT, LEGACY_MIGRATION_GROUP);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    public LocalDateTime getScheduledMigrationNextRunAt() {
        if (!isScheduledMigrationEnabled()) {
            return null;
        }
        LocalDateTime lastRunAt = getScheduledMigrationLastRunAt();
        if (lastRunAt == null) {
            return LocalDateTime.now();
        }
        return lastRunAt.plusMinutes(getScheduledMigrationIntervalMinutes());
    }

    public void updateScheduledMigrationSettings(boolean enabled, int intervalMinutes) {
        settingService.upsertSetting(ENABLED, LEGACY_MIGRATION_GROUP, String.valueOf(enabled));
        settingService.upsertSetting(INTERVAL_MINUTES, LEGACY_MIGRATION_GROUP, String.valueOf(Math.max(5, Math.min(intervalMinutes <= 0 ? 1440 : intervalMinutes, 10080))));
    }

    public MembershipMigrationResult runScheduledMigrationNow() {
        markScheduledMigrationRun();
        return migrateMemberships();
    }

    private boolean isScheduledMigrationDue() {
        LocalDateTime nextRunAt = getScheduledMigrationNextRunAt();
        return nextRunAt != null && !nextRunAt.isAfter(LocalDateTime.now());
    }

    private void markScheduledMigrationRun() {
        settingService.upsertSetting(LAST_RUN_AT, LEGACY_MIGRATION_GROUP, LocalDateTime.now().toString());
    }

    public MembershipMigrationResult migrateMemberships() {
        MembershipMigrationResult result = new MembershipMigrationResult();
        TransactionViewDto transactionViewDto = new TransactionViewDto();
        transactionViewDto.setType(TransactionType.MEMBERSHIP);

        for (TenantDto tenant : tenantAdminService.getAll()) {
            MembershipMigrationResult.TenantResult tenantResult = new MembershipMigrationResult.TenantResult();
            tenantResult.setTenantId(tenant == null ? null : tenant.getId());
            result.getTenants().add(tenantResult);

            try {
                if (tenant == null || isBlank(tenant.getId())) {
                    tenantResult.addError("Tenant id is missing");
                    continue;
                }

                TenantContext.setCurrentTenant(tenant.getId());
                migrateMembershipPlans(tenantResult);

                List<TransactionViewEntity> transactionViewEntities = transactionService.searchV2(transactionViewDto);
                tenantResult.setOldMembershipsFound(transactionViewEntities.size());

                for (TransactionViewEntity transactionViewEntity : transactionViewEntities) {
                    try {
                        migrateSingleMembership(transactionViewEntity, tenantResult);
                    } catch (Exception e) {
                        tenantResult.setMembershipsFailed(tenantResult.getMembershipsFailed() + 1);
                        tenantResult.addError("Membership " + safe(transactionViewEntity == null ? null : transactionViewEntity.getTransactionId()) + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                tenantResult.addError("Tenant migration failed: " + e.getMessage());
            } finally {
                TenantContext.clear();
                result.rollup(tenantResult);
            }
        }

        return result;
    }

    private void migrateSingleMembership(TransactionViewEntity oldMembership, MembershipMigrationResult.TenantResult tenantResult) {
        if (oldMembership == null || isBlank(oldMembership.getTransactionId())) {
            tenantResult.setMembershipsSkipped(tenantResult.getMembershipsSkipped() + 1);
            tenantResult.addError("Skipped old membership with missing transaction id");
            return;
        }

        if (isBlank(oldMembership.getMainPartnerId())) {
            tenantResult.setMembershipsSkipped(tenantResult.getMembershipsSkipped() + 1);
            tenantResult.addError("Membership " + oldMembership.getTransactionId() + " skipped because main partner is missing");
            return;
        }

        if (isBlank(oldMembership.getProductId())) {
            tenantResult.setMembershipsSkipped(tenantResult.getMembershipsSkipped() + 1);
            tenantResult.addError("Membership " + oldMembership.getTransactionId() + " skipped because product is missing");
            return;
        }

        MembershipPlanEntity membershipPlanEntity = membershipPlanRepository.findByOldId(oldMembership.getProductId()).orElse(null);
        if (membershipPlanEntity == null) {
            tenantResult.setMembershipsSkipped(tenantResult.getMembershipsSkipped() + 1);
            tenantResult.addError("Membership " + oldMembership.getTransactionId() + " skipped because plan was not migrated for old product " + oldMembership.getProductId());
            return;
        }

        boolean created = false;
        MembershipEntity membership = membershipRepository.findByOldId(oldMembership.getTransactionId()).orElse(null);
        if (membership == null && !isBlank(oldMembership.getTransactionNumber())) {
            membership = membershipRepository.findByMembershipNo(oldMembership.getTransactionNumber()).orElse(null);
        }

        if (membership == null) {
            membership = new MembershipEntity();
            membership.setOldId(oldMembership.getTransactionId());
            membership.setMembershipNo(firstNonBlank(oldMembership.getTransactionNumber(), oldMembership.getTransactionId()));
            membership.setCreatedAt(toLocalDateTime(oldMembership.getCreationDate()));
            membership.setCreatedBy(oldMembership.getCreatedById());
            created = true;
        }

        membership.setOldId(firstNonBlank(membership.getOldId(), oldMembership.getTransactionId()));
        membership.setMemberId(oldMembership.getMainPartnerId());
        membership.setPlanId(membershipPlanEntity.getId());
        membership.setPremiumCents(membershipPlanEntity.getPremiumCents());
        membership.setStatus(firstNonBlank(oldMembership.getTransactionStatus(), "ACTIVE"));
        membership.setStartDate(toLocalDate(oldMembership.getCreationDate(), LocalDate.now()));
        membership.setJoinDate(toLocalDate(oldMembership.getCreationDate(), membership.getStartDate()));
        membership.setUpdatedAt(LocalDateTime.now());
        membership.setUpdatedBy(oldMembership.getChangedById());

        try {
            membership.setPaidUpToPeriod(premiumService.getPaidUpToPeriod(oldMembership.getTransactionId()));
        } catch (Exception e) {
            tenantResult.addWarning("Unable to calculate paid-up period for " + oldMembership.getTransactionId() + ": " + e.getMessage());
        }

        MembershipEntity savedMembership = membershipRepository.save(membership);
        if (created) {
            tenantResult.setMembershipsCreated(tenantResult.getMembershipsCreated() + 1);
        } else {
            tenantResult.setMembershipsUpdated(tenantResult.getMembershipsUpdated() + 1);
        }

        migrateDependentsForMembership(oldMembership, savedMembership, tenantResult);
    }

    private void migrateDependentsForMembership(TransactionViewEntity oldMembership,
                                                MembershipEntity membership,
                                                MembershipMigrationResult.TenantResult tenantResult) {
        List<TransactionPartnerEntity> partners = transactionPartnerRepository.findPartnerByTransaction(oldMembership.getTransactionId());
        Set<String> processedDependentPartnerIds = new HashSet<>();

        for (TransactionPartnerEntity oldPartner : partners) {
            TransactionPartnerPKEntity pk = oldPartner.getTransactionPartnerPKEntity();
            if (pk == null || !isDependentFunction(pk.getFunction())) {
                continue;
            }

            String dependentPartnerId = trimToNull(pk.getPartner());
            if (dependentPartnerId == null) {
                tenantResult.setDependentsSkipped(tenantResult.getDependentsSkipped() + 1);
                tenantResult.addWarning("Membership " + oldMembership.getTransactionId() + " has dependent row with missing partner id");
                continue;
            }

            if (!processedDependentPartnerIds.add(dependentPartnerId)) {
                tenantResult.setDependentsDuplicateSkipped(tenantResult.getDependentsDuplicateSkipped() + 1);
                continue;
            }

            if (!partnerRepository.existsById(dependentPartnerId)) {
                tenantResult.setDependentsSkipped(tenantResult.getDependentsSkipped() + 1);
                tenantResult.addWarning("Membership " + oldMembership.getTransactionId() + " dependent partner does not exist: " + dependentPartnerId);
                continue;
            }

            if (membershipDependentRepository.existsByMembershipIdAndDependentPartnerId(membership.getId(), dependentPartnerId)) {
                tenantResult.setDependentsAlreadyExisting(tenantResult.getDependentsAlreadyExisting() + 1);
                continue;
            }

            MembershipDependentEntity membershipDependentEntity = new MembershipDependentEntity();
            membershipDependentEntity.setMembershipId(membership.getId());
            membershipDependentEntity.setDependentPartnerId(dependentPartnerId);
            membershipDependentEntity.setDependentType(DependentType.ANY);
            membershipDependentEntity.setActive(isActive(oldPartner.getStatus()));
            membershipDependentEntity.setCreatedAt(toLocalDateTime(oldPartner.getDateAdded(), membership.getCreatedAt()));
            membershipDependentEntity.setUpdatedAt(toLocalDateTime(oldPartner.getDateEffective(), membership.getUpdatedAt()));
            membershipDependentEntity.setCreatedBy(firstNonBlank(oldPartner.getCreatedBy(), membership.getCreatedBy()));
            membershipDependentEntity.setUpdatedBy(firstNonBlank(oldPartner.getChangedBy(), membership.getUpdatedBy()));
            membershipDependentRepository.save(membershipDependentEntity);
            tenantResult.setDependentsCreated(tenantResult.getDependentsCreated() + 1);
        }
    }

    private boolean isDependentFunction(String function) {
        return function != null && DEPENDENT_PARTNER_FUNCTIONS.contains(function.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isActive(String status) {
        if (status == null) {
            return true;
        }
        String normalised = status.trim().toUpperCase(Locale.ROOT);
        return !("INACTIVE".equals(normalised)
                || "CANCELLED".equals(normalised)
                || "CANCELED".equals(normalised)
                || "DELETED".equals(normalised)
                || "REMOVED".equals(normalised));
    }

    private void incrementPlanFailure(MembershipMigrationResult.TenantResult tenantResult, String message) {
        if (tenantResult != null) {
            tenantResult.setPlansFailed(tenantResult.getPlansFailed() + 1);
            tenantResult.addError(message);
        } else {
            System.err.println(message);
        }
    }

    private LocalDate toLocalDate(Date date, LocalDate fallback) {
        if (date == null) {
            return fallback;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date, LocalDateTime.now());
    }

    private LocalDateTime toLocalDateTime(Date date, LocalDateTime fallback) {
        if (date == null) {
            return fallback == null ? LocalDateTime.now() : fallback;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String safe(String value) {
        return value == null ? "<null>" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class MembershipMigrationResult {
        private int tenantsProcessed;
        private int oldMembershipsFound;
        private int membershipsCreated;
        private int membershipsUpdated;
        private int membershipsSkipped;
        private int membershipsFailed;
        private int dependentsCreated;
        private int dependentsAlreadyExisting;
        private int dependentsDuplicateSkipped;
        private int dependentsSkipped;
        private final List<TenantResult> tenants = new ArrayList<>();

        public void rollup(TenantResult tenantResult) {
            tenantsProcessed++;
            oldMembershipsFound += tenantResult.getOldMembershipsFound();
            membershipsCreated += tenantResult.getMembershipsCreated();
            membershipsUpdated += tenantResult.getMembershipsUpdated();
            membershipsSkipped += tenantResult.getMembershipsSkipped();
            membershipsFailed += tenantResult.getMembershipsFailed();
            dependentsCreated += tenantResult.getDependentsCreated();
            dependentsAlreadyExisting += tenantResult.getDependentsAlreadyExisting();
            dependentsDuplicateSkipped += tenantResult.getDependentsDuplicateSkipped();
            dependentsSkipped += tenantResult.getDependentsSkipped();
        }

        public int getTenantsProcessed() { return tenantsProcessed; }
        public int getOldMembershipsFound() { return oldMembershipsFound; }
        public int getMembershipsCreated() { return membershipsCreated; }
        public int getMembershipsUpdated() { return membershipsUpdated; }
        public int getMembershipsSkipped() { return membershipsSkipped; }
        public int getMembershipsFailed() { return membershipsFailed; }
        public int getDependentsCreated() { return dependentsCreated; }
        public int getDependentsAlreadyExisting() { return dependentsAlreadyExisting; }
        public int getDependentsDuplicateSkipped() { return dependentsDuplicateSkipped; }
        public int getDependentsSkipped() { return dependentsSkipped; }
        public List<TenantResult> getTenants() { return tenants; }

        public static class TenantResult {
            private String tenantId;
            private int plansCreated;
            private int plansAlreadyExisting;
            private int plansFailed;
            private int oldMembershipsFound;
            private int membershipsCreated;
            private int membershipsUpdated;
            private int membershipsSkipped;
            private int membershipsFailed;
            private int dependentsCreated;
            private int dependentsAlreadyExisting;
            private int dependentsDuplicateSkipped;
            private int dependentsSkipped;
            private final List<String> warnings = new ArrayList<>();
            private final List<String> errors = new ArrayList<>();

            public void addWarning(String warning) {
                if (warnings.size() < 50) {
                    warnings.add(warning);
                }
            }

            public void addError(String error) {
                if (errors.size() < 50) {
                    errors.add(error);
                }
            }

            public String getTenantId() { return tenantId; }
            public void setTenantId(String tenantId) { this.tenantId = tenantId; }
            public int getPlansCreated() { return plansCreated; }
            public void setPlansCreated(int plansCreated) { this.plansCreated = plansCreated; }
            public int getPlansAlreadyExisting() { return plansAlreadyExisting; }
            public void setPlansAlreadyExisting(int plansAlreadyExisting) { this.plansAlreadyExisting = plansAlreadyExisting; }
            public int getPlansFailed() { return plansFailed; }
            public void setPlansFailed(int plansFailed) { this.plansFailed = plansFailed; }
            public int getOldMembershipsFound() { return oldMembershipsFound; }
            public void setOldMembershipsFound(int oldMembershipsFound) { this.oldMembershipsFound = oldMembershipsFound; }
            public int getMembershipsCreated() { return membershipsCreated; }
            public void setMembershipsCreated(int membershipsCreated) { this.membershipsCreated = membershipsCreated; }
            public int getMembershipsUpdated() { return membershipsUpdated; }
            public void setMembershipsUpdated(int membershipsUpdated) { this.membershipsUpdated = membershipsUpdated; }
            public int getMembershipsSkipped() { return membershipsSkipped; }
            public void setMembershipsSkipped(int membershipsSkipped) { this.membershipsSkipped = membershipsSkipped; }
            public int getMembershipsFailed() { return membershipsFailed; }
            public void setMembershipsFailed(int membershipsFailed) { this.membershipsFailed = membershipsFailed; }
            public int getDependentsCreated() { return dependentsCreated; }
            public void setDependentsCreated(int dependentsCreated) { this.dependentsCreated = dependentsCreated; }
            public int getDependentsAlreadyExisting() { return dependentsAlreadyExisting; }
            public void setDependentsAlreadyExisting(int dependentsAlreadyExisting) { this.dependentsAlreadyExisting = dependentsAlreadyExisting; }
            public int getDependentsDuplicateSkipped() { return dependentsDuplicateSkipped; }
            public void setDependentsDuplicateSkipped(int dependentsDuplicateSkipped) { this.dependentsDuplicateSkipped = dependentsDuplicateSkipped; }
            public int getDependentsSkipped() { return dependentsSkipped; }
            public void setDependentsSkipped(int dependentsSkipped) { this.dependentsSkipped = dependentsSkipped; }
            public List<String> getWarnings() { return warnings; }
            public List<String> getErrors() { return errors; }
        }
    }
}
