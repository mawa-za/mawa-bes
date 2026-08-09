package za.co.mawa.bes.enums;

import za.co.mawa.bes.dto.product.classification.ProductTypeDefinitionDto;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum ProductTypeCode {
    PHYSICAL_PRODUCT(
            "PHYSICAL-PRODUCT", "Physical Product",
            "Stocked product that can be received, put away, issued or sold.",
            true, true, true, false, true, false, false, false, true),
    CONSUMABLE(
            "CONSUMABLE", "Consumable",
            "Stocked item consumed internally or while delivering a service.",
            true, true, true, true, false, false, false, false, false),
    SERVICE(
            "SERVICE", "Service",
            "Non-stock service such as transport, catering, grave digging or installation.",
            false, false, false, false, false, false, false, false, true),
    ASSET(
            "ASSET", "Asset",
            "Durable item registered and tracked through Asset Management.",
            false, true, false, false, true, true, false, false, false),
    FUNERAL_PACKAGE(
            "FUNERAL-PACKAGE", "Funeral Package",
            "Bundled funeral offering made up of products and services.",
            false, false, false, false, false, false, true, true, true),
    GROUP_SOCIETY(
            "GROUP-SOCIETY", "Group Society",
            "Cover product selected when a group society is registered.",
            false, false, false, false, false, false, false, true, false),
    TOMBSTONE(
            "TOMBSTONE", "Tombstone",
            "Physical memorial product using specialised sales, installation, layby and cover workflows.",
            true, true, true, false, true, false, false, true, true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean stockControlled;
    private final boolean canBeReceived;
    private final boolean canBePutAway;
    private final boolean consumedOnIssue;
    private final boolean returnable;
    private final boolean assetTracked;
    private final boolean bundle;
    private final boolean specialisedWorkflow;
    private final boolean defaultAvailableForSale;

    ProductTypeCode(String code,
                    String displayName,
                    String description,
                    boolean stockControlled,
                    boolean canBeReceived,
                    boolean canBePutAway,
                    boolean consumedOnIssue,
                    boolean returnable,
                    boolean assetTracked,
                    boolean bundle,
                    boolean specialisedWorkflow,
                    boolean defaultAvailableForSale) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.stockControlled = stockControlled;
        this.canBeReceived = canBeReceived;
        this.canBePutAway = canBePutAway;
        this.consumedOnIssue = consumedOnIssue;
        this.returnable = returnable;
        this.assetTracked = assetTracked;
        this.bundle = bundle;
        this.specialisedWorkflow = specialisedWorkflow;
        this.defaultAvailableForSale = defaultAvailableForSale;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isStockControlled() {
        return stockControlled;
    }

    public boolean isCanBeReceived() {
        return canBeReceived;
    }

    public boolean isCanBePutAway() {
        return canBePutAway;
    }

    public boolean isConsumedOnIssue() {
        return consumedOnIssue;
    }

    public boolean isReturnable() {
        return returnable;
    }

    public boolean isAssetTracked() {
        return assetTracked;
    }

    public boolean isBundle() {
        return bundle;
    }

    public boolean isSpecialisedWorkflow() {
        return specialisedWorkflow;
    }

    public boolean isDefaultAvailableForSale() {
        return defaultAvailableForSale;
    }

    public ProductTypeDefinitionDto toDto() {
        return ProductTypeDefinitionDto.builder()
                .code(code)
                .name(displayName)
                .description(description)
                .stockControlled(stockControlled)
                .canBeReceived(canBeReceived)
                .canBePutAway(canBePutAway)
                .consumedOnIssue(consumedOnIssue)
                .returnable(returnable)
                .assetTracked(assetTracked)
                .bundle(bundle)
                .specialisedWorkflow(specialisedWorkflow)
                .defaultAvailableForSale(defaultAvailableForSale)
                .build();
    }

    public static List<ProductTypeDefinitionDto> definitions() {
        return Arrays.stream(values()).map(ProductTypeCode::toDto).toList();
    }

    public static Optional<ProductTypeCode> find(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        String code = rawCode.trim().toUpperCase(Locale.ROOT).replace('_', '-');
        if ("GENERAL".equals(code)) {
            code = PHYSICAL_PRODUCT.code;
        } else if ("CONSUMABLES".equals(code)) {
            code = CONSUMABLE.code;
        }
        String finalCode = code;
        return Arrays.stream(values()).filter(value -> value.code.equals(finalCode)).findFirst();
    }

    public static ProductTypeCode requireSelectable(String rawCode) {
        return find(rawCode).orElseThrow(() -> new IllegalArgumentException(
                "Unsupported product type. Select Physical Product, Consumable, Service, Asset, Funeral Package, Group Society or Tombstone."));
    }
}
