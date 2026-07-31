package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AssetHireService {

    private static final Set<String> BLOCKED_ASSET_CONDITIONS = Set.of("DAMAGED", "POOR", "LOST");
    private static final Set<String> VALID_ASSET_CONDITIONS = Set.of("NEW", "GOOD", "FAIR", "POOR", "DAMAGED", "LOST");
    private final JdbcTemplate jdbcTemplate;

    public AssetHireService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getLinkedAssets(String serviceProductId, LocalDateTime startAt, LocalDateTime endAt) {
        requireServiceProduct(serviceProductId);
        List<Map<String, Object>> links = jdbcTemplate.queryForList("""
                SELECT l.id AS link_id, l.service_product_id, l.asset_id, l.capacity, l.active, l.notes AS link_notes,
                       a.asset_no, a.barcode, a.name, a.description, a.category, a.serial_no, a.location,
                       a.status, a.condition_status
                  FROM product_asset_link l
                  JOIN asset_register a ON a.id = l.asset_id
                 WHERE l.service_product_id = ?
                 ORDER BY l.active DESC, a.name, a.asset_no
                """, serviceProductId);
        for (Map<String, Object> link : links) {
            String assetId = Objects.toString(link.get("asset_id"), "");
            int capacity = asInt(link.get("capacity"), 1);
            int reserved = startAt == null || endAt == null
                    ? currentReservedQuantity(assetId)
                    : reservedQuantity(assetId, startAt, endAt, null);
            boolean assetAvailable = "ACTIVE".equalsIgnoreCase(Objects.toString(link.get("status"), ""))
                    && !BLOCKED_ASSET_CONDITIONS.contains(Objects.toString(link.get("condition_status"), "").toUpperCase(Locale.ROOT));
            link.put("reserved_quantity", reserved);
            link.put("available_capacity", assetAvailable && Boolean.TRUE.equals(asBoolean(link.get("active")))
                    ? Math.max(0, capacity - reserved) : 0);
            link.put("available", assetAvailable && Boolean.TRUE.equals(asBoolean(link.get("active"))) && reserved < capacity);
        }
        return links;
    }

    @Transactional
    public List<Map<String, Object>> replaceLinkedAssets(String serviceProductId, AssetLinkSetRequest request, String userId) {
        requireServiceProduct(serviceProductId);
        List<AssetLinkRequest> requestedLinks = request == null || request.assets() == null ? List.of() : request.assets();
        Set<String> seen = new HashSet<>();

        jdbcTemplate.update("UPDATE product_asset_link SET active = 0, updated_at = CURRENT_TIMESTAMP, updated_by = ? WHERE service_product_id = ?",
                blank(userId), serviceProductId);

        for (AssetLinkRequest link : requestedLinks) {
            if (link == null || !StringUtils.hasText(link.assetId())) {
                throw new IllegalArgumentException("assetId is required for every linked hire asset.");
            }
            String assetId = link.assetId().trim();
            if (!seen.add(assetId)) {
                throw new IllegalArgumentException("An asset may only be linked once to a service product.");
            }
            int capacity = link.capacity() == null ? 1 : link.capacity();
            if (capacity <= 0) throw new IllegalArgumentException("Asset capacity must be greater than zero.");
            requireAsset(assetId);
            jdbcTemplate.update("""
                    INSERT INTO product_asset_link
                    (id, service_product_id, asset_id, capacity, active, notes, created_at, created_by, updated_at, updated_by)
                    VALUES (?, ?, ?, ?, 1, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)
                    ON DUPLICATE KEY UPDATE capacity = VALUES(capacity), active = 1, notes = VALUES(notes),
                                            updated_at = CURRENT_TIMESTAMP, updated_by = VALUES(updated_by)
                    """, UUID.randomUUID().toString(), serviceProductId, assetId, capacity,
                    blank(link.notes()), blank(userId), blank(userId));
        }
        return getLinkedAssets(serviceProductId, null, null);
    }

    public List<Map<String, Object>> listReservations(String query, String status, LocalDateTime from, LocalDateTime to) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.*, a.asset_no, a.name AS asset_name, a.status AS asset_status,
                       a.condition_status AS asset_condition, p.code AS service_product_code,
                       p.description AS service_product_description
                  FROM asset_reservation r
                  JOIN asset_register a ON a.id = r.asset_id
                  JOIN product p ON p.id = r.service_product_id
                 WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND r.status = ?");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        if (from != null) {
            sql.append(" AND r.end_at > ?");
            args.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND r.start_at < ?");
            args.add(Timestamp.valueOf(to));
        }
        if (StringUtils.hasText(query)) {
            sql.append(" AND (UPPER(a.asset_no) LIKE ? OR UPPER(a.name) LIKE ? OR UPPER(p.code) LIKE ? OR UPPER(p.description) LIKE ? OR UPPER(COALESCE(r.source_reference,'')) LIKE ?)");
            String like = "%" + query.trim().toUpperCase(Locale.ROOT) + "%";
            Collections.addAll(args, like, like, like, like, like);
        }
        sql.append(" ORDER BY r.start_at DESC, r.created_at DESC LIMIT 1000");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public Map<String, Object> createReservation(ReservationRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Reservation details are required.");
        requireWindow(request.startAt(), request.endAt());
        requireServiceProduct(request.serviceProductId());
        requireAsset(request.assetId());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        if (quantity <= 0) throw new IllegalArgumentException("Reservation quantity must be greater than zero.");

        Map<String, Object> link = lockActiveLink(request.serviceProductId(), request.assetId());
        assertAssetOperational(request.assetId());
        int capacity = asInt(link.get("capacity"), 1);
        int alreadyReserved = reservedQuantity(request.assetId(), request.startAt(), request.endAt(), null);
        if (alreadyReserved + quantity > capacity) {
            throw new IllegalArgumentException("Asset capacity is not available for the selected date and time. Available capacity: "
                    + Math.max(0, capacity - alreadyReserved) + ".");
        }
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO asset_reservation
                (id, asset_id, service_product_id, reserved_quantity, source_type, source_id, source_reference,
                 customer_partner_id, start_at, end_at, status, notes, created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'RESERVED', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)
                """, id, request.assetId(), request.serviceProductId(), quantity,
                upper(defaultValue(request.sourceType(), "MANUAL")), blank(request.sourceId()), blank(request.sourceReference()),
                blank(request.customerPartnerId()), Timestamp.valueOf(request.startAt()), Timestamp.valueOf(request.endAt()),
                blank(request.notes()), blank(userId), blank(userId));
        event(request.assetId(), "RESERVE", null, id,
                "Reserved " + quantity + " unit(s) from " + request.startAt() + " to " + request.endAt(), userId);
        return getReservation(id);
    }

    @Transactional
    public Map<String, Object> issue(String id, ConditionRequest request, String userId) {
        Map<String, Object> reservation = lockReservation(id);
        requireStatus(reservation, "RESERVED");
        assertAssetOperational(Objects.toString(reservation.get("asset_id"), ""));
        String condition = normalizeCondition(request == null ? null : request.condition(),
                Objects.toString(reservation.get("asset_condition"), "GOOD"));
        if (BLOCKED_ASSET_CONDITIONS.contains(condition)) {
            throw new IllegalArgumentException("An asset in " + condition + " condition cannot be issued.");
        }
        jdbcTemplate.update("""
                UPDATE asset_reservation
                   SET status = 'ISSUED', issue_condition = ?, issued_at = CURRENT_TIMESTAMP,
                       notes = COALESCE(?, notes), updated_at = CURRENT_TIMESTAMP, updated_by = ?
                 WHERE id = ?
                """, condition, blank(request == null ? null : request.notes()), blank(userId), id);
        event(Objects.toString(reservation.get("asset_id"), ""), "HIRE_ISSUE", "RESERVED", "ISSUED",
                request == null ? null : request.notes(), userId);
        return getReservation(id);
    }

    @Transactional
    public Map<String, Object> returnAsset(String id, ReturnRequest request, String userId) {
        Map<String, Object> reservation = lockReservation(id);
        requireStatus(reservation, "ISSUED");
        String assetId = Objects.toString(reservation.get("asset_id"), "");
        String condition = normalizeCondition(request == null ? null : request.condition(), "GOOD");
        boolean lost = (request != null && Boolean.TRUE.equals(request.lost())) || "LOST".equals(condition);
        String assetStatus = lost ? "LOST" : (Set.of("DAMAGED", "POOR").contains(condition) ? "IN_REPAIR" : "ACTIVE");
        jdbcTemplate.update("""
                UPDATE asset_reservation
                   SET status = 'RETURNED', return_condition = ?, damage_notes = ?, returned_at = CURRENT_TIMESTAMP,
                       notes = COALESCE(?, notes), updated_at = CURRENT_TIMESTAMP, updated_by = ?
                 WHERE id = ?
                """, condition, blank(request == null ? null : request.damageNotes()),
                blank(request == null ? null : request.notes()), blank(userId), id);
        jdbcTemplate.update("UPDATE asset_register SET status = ?, condition_status = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ? WHERE id = ?",
                assetStatus, lost ? "LOST" : condition, blank(userId), assetId);
        event(assetId, lost ? "HIRE_LOST" : "HIRE_RETURN", "ISSUED", assetStatus,
                request == null ? null : firstText(request.damageNotes(), request.notes()), userId);
        return getReservation(id);
    }

    @Transactional
    public Map<String, Object> cancel(String id, String notes, String userId) {
        Map<String, Object> reservation = lockReservation(id);
        String status = Objects.toString(reservation.get("status"), "").toUpperCase(Locale.ROOT);
        if (!"RESERVED".equals(status)) {
            throw new IllegalArgumentException("Only reserved, not-yet-issued assets can be cancelled.");
        }
        jdbcTemplate.update("""
                UPDATE asset_reservation
                   SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP, notes = COALESCE(?, notes),
                       updated_at = CURRENT_TIMESTAMP, updated_by = ?
                 WHERE id = ?
                """, blank(notes), blank(userId), id);
        event(Objects.toString(reservation.get("asset_id"), ""), "HIRE_CANCEL", "RESERVED", "CANCELLED", notes, userId);
        return getReservation(id);
    }

    @Transactional
    public void synchronizeFuneralServiceReservations(String funeralServiceId,
                                                      String packageId,
                                                      LocalDate funeralDate,
                                                      String customerPartnerId,
                                                      String reference) {
        if (!StringUtils.hasText(funeralServiceId)) return;
        jdbcTemplate.update("""
                UPDATE asset_reservation
                   SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP,
                       notes = CONCAT(COALESCE(notes,''), CASE WHEN COALESCE(notes,'')='' THEN '' ELSE '\n' END,
                                      'Automatically replaced after funeral arrangement change')
                 WHERE source_type = 'FUNERAL_SERVICE' AND source_id = ? AND status = 'RESERVED'
                """, funeralServiceId);
        if (!StringUtils.hasText(packageId) || funeralDate == null) return;

        LocalDateTime startAt = funeralDate.atStartOfDay();
        LocalDateTime endAt = funeralDate.plusDays(1).atStartOfDay();
        List<Map<String, Object>> serviceItems = jdbcTemplate.queryForList("""
                SELECT fpi.product_id, fpi.product_code, fpi.product_description, fpi.quantity
                  FROM funeral_package_item fpi
                  JOIN product p ON p.id = fpi.product_id
                 WHERE fpi.funeral_package_id = ? AND p.type = 'SERVICE'
                 ORDER BY fpi.product_description
                """, packageId);

        for (Map<String, Object> item : serviceItems) {
            String productId = Objects.toString(item.get("product_id"), "");
            int requiredQuantity = asInt(item.get("quantity"), 0);
            List<Map<String, Object>> links = jdbcTemplate.queryForList("""
                    SELECT l.asset_id, l.capacity, a.asset_no, a.name
                      FROM product_asset_link l
                      JOIN asset_register a ON a.id = l.asset_id
                     WHERE l.service_product_id = ? AND l.active = 1
                       AND a.status = 'ACTIVE' AND a.condition_status NOT IN ('DAMAGED','POOR','LOST')
                     ORDER BY l.capacity DESC, a.asset_no
                    """, productId);
            if (links.isEmpty()) {
                // Many services (collection, storage, decoration) do not require a tracked reusable asset.
                continue;
            }

            int remaining = requiredQuantity;
            for (Map<String, Object> link : links) {
                if (remaining <= 0) break;
                String assetId = Objects.toString(link.get("asset_id"), "");
                lockActiveLink(productId, assetId);
                int capacity = asInt(link.get("capacity"), 1);
                int reserved = reservedQuantity(assetId, startAt, endAt, null);
                int available = Math.max(0, capacity - reserved);
                if (available == 0) continue;
                int allocated = Math.min(remaining, available);
                createReservation(new ReservationRequest(assetId, productId, allocated,
                        "FUNERAL_SERVICE", funeralServiceId, reference, customerPartnerId,
                        startAt, endAt, "Automatically reserved from funeral package " + packageId), "SYSTEM");
                remaining -= allocated;
            }
            if (remaining > 0) {
                throw new IllegalArgumentException("Insufficient available hire assets for "
                        + Objects.toString(item.get("product_description"), Objects.toString(item.get("product_code"), "service"))
                        + " on " + funeralDate + ". Required: " + requiredQuantity + ", unallocated: " + remaining + ".");
            }
        }
    }

    private Map<String, Object> getReservation(String id) {
        return jdbcTemplate.queryForMap("""
                SELECT r.*, a.asset_no, a.name AS asset_name, a.status AS asset_status,
                       a.condition_status AS asset_condition, p.code AS service_product_code,
                       p.description AS service_product_description
                  FROM asset_reservation r
                  JOIN asset_register a ON a.id = r.asset_id
                  JOIN product p ON p.id = r.service_product_id
                 WHERE r.id = ?
                """, id);
    }

    private Map<String, Object> lockReservation(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT r.*, a.status AS asset_status, a.condition_status AS asset_condition
                  FROM asset_reservation r
                  JOIN asset_register a ON a.id = r.asset_id
                 WHERE r.id = ? FOR UPDATE
                """, id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Asset reservation not found: " + id);
        return rows.get(0);
    }

    private Map<String, Object> lockActiveLink(String serviceProductId, String assetId) {
        List<Map<String, Object>> links = jdbcTemplate.queryForList("""
                SELECT * FROM product_asset_link
                 WHERE service_product_id = ? AND asset_id = ? AND active = 1
                 FOR UPDATE
                """, serviceProductId, assetId);
        if (links.isEmpty()) {
            throw new IllegalArgumentException("The selected asset is not actively linked to this hire service product.");
        }
        return links.get(0);
    }

    private int currentReservedQuantity(String assetId) {
        Integer quantity = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(reserved_quantity),0)
                  FROM asset_reservation
                 WHERE asset_id = ? AND status IN ('RESERVED','ISSUED') AND start_at <= CURRENT_TIMESTAMP AND end_at > CURRENT_TIMESTAMP
                """, Integer.class, assetId);
        return quantity == null ? 0 : quantity;
    }

    private int reservedQuantity(String assetId, LocalDateTime startAt, LocalDateTime endAt, String excludeId) {
        String sql = "SELECT COALESCE(SUM(reserved_quantity),0) FROM asset_reservation "
                + "WHERE asset_id = ? AND status IN ('RESERVED','ISSUED') AND start_at < ? AND end_at > ?";
        List<Object> args = new ArrayList<>(List.of(assetId, Timestamp.valueOf(endAt), Timestamp.valueOf(startAt)));
        if (StringUtils.hasText(excludeId)) {
            sql += " AND id <> ?";
            args.add(excludeId);
        }
        Integer quantity = jdbcTemplate.queryForObject(sql, Integer.class, args.toArray());
        return quantity == null ? 0 : quantity;
    }

    private void requireServiceProduct(String productId) {
        if (!StringUtils.hasText(productId)) throw new IllegalArgumentException("serviceProductId is required.");
        List<String> types = jdbcTemplate.queryForList("SELECT type FROM product WHERE id = ?", String.class, productId);
        if (types.isEmpty()) throw new IllegalArgumentException("Service product not found: " + productId);
        if (!"SERVICE".equalsIgnoreCase(types.get(0))) {
            throw new IllegalArgumentException("Reusable assets may only be linked to products of type Service.");
        }
    }

    private void requireAsset(String assetId) {
        if (!StringUtils.hasText(assetId)) throw new IllegalArgumentException("assetId is required.");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM asset_register WHERE id = ?", Integer.class, assetId);
        if (count == null || count == 0) throw new IllegalArgumentException("Asset not found: " + assetId);
    }

    private void assertAssetOperational(String assetId) {
        Map<String, Object> asset = jdbcTemplate.queryForMap(
                "SELECT status, condition_status FROM asset_register WHERE id = ? FOR UPDATE", assetId);
        String status = Objects.toString(asset.get("status"), "").toUpperCase(Locale.ROOT);
        String condition = Objects.toString(asset.get("condition_status"), "").toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(status)) throw new IllegalArgumentException("Asset is not active and cannot be reserved or issued.");
        if (BLOCKED_ASSET_CONDITIONS.contains(condition)) {
            throw new IllegalArgumentException("Asset condition is " + condition + " and it cannot be reserved or issued.");
        }
    }

    private void requireWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) throw new IllegalArgumentException("Reservation start and end are required.");
        if (!endAt.isAfter(startAt)) throw new IllegalArgumentException("Reservation end must be after its start.");
    }

    private void requireStatus(Map<String, Object> reservation, String expected) {
        String current = Objects.toString(reservation.get("status"), "").toUpperCase(Locale.ROOT);
        if (!expected.equals(current)) {
            throw new IllegalArgumentException("Reservation must be " + expected + " before this action. Current status: " + current + ".");
        }
    }

    private void event(String assetId, String type, String oldValue, String newValue, String notes, String userId) {
        jdbcTemplate.update("""
                INSERT INTO asset_register_event
                (id, asset_id, event_type, old_value, new_value, notes, created_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                """, UUID.randomUUID().toString(), assetId, type, oldValue, newValue, blank(notes), blank(userId));
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(Objects.toString(value, "false"));
    }

    private int asInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(Objects.toString(value, "")); }
        catch (Exception ignored) { return defaultValue; }
    }

    private String normalizeCondition(String value, String fallback) {
        String condition = upper(defaultValue(value, fallback));
        if (!VALID_ASSET_CONDITIONS.contains(condition)) {
            throw new IllegalArgumentException("Asset condition must be NEW, GOOD, FAIR, POOR, DAMAGED or LOST.");
        }
        return condition;
    }

    private String blank(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String upper(String value) { return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null; }
    private String defaultValue(String value, String fallback) { return StringUtils.hasText(value) ? value : fallback; }
    private String firstText(String first, String second) { return StringUtils.hasText(first) ? first : second; }

    public record AssetLinkSetRequest(List<AssetLinkRequest> assets) {}
    public record AssetLinkRequest(String assetId, Integer capacity, String notes) {}
    public record ReservationRequest(String assetId, String serviceProductId, Integer quantity,
                                     String sourceType, String sourceId, String sourceReference,
                                     String customerPartnerId, LocalDateTime startAt, LocalDateTime endAt,
                                     String notes) {}
    public record ConditionRequest(String condition, String notes) {}
    public record ReturnRequest(String condition, Boolean lost, String damageNotes, String notes) {}
    public record CancelRequest(String notes) {}
}
