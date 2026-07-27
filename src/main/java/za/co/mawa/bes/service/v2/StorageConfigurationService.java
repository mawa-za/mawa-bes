package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class StorageConfigurationService {
    private final JdbcTemplate jdbcTemplate;

    public StorageConfigurationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> warehouses(boolean activeOnly) {
        return jdbcTemplate.queryForList("""
                SELECT id,
                       warehouse_code AS code,
                       name,
                       description,
                       CASE WHEN UPPER(COALESCE(status, 'ACTIVE')) = 'ACTIVE' THEN 1 ELSE 0 END AS active,
                       created_at,
                       updated_at
                  FROM warehouse
                """ + (activeOnly ? " WHERE UPPER(COALESCE(status, 'ACTIVE')) = 'ACTIVE' " : " ")
                + " ORDER BY name");
    }

    public List<Map<String, Object>> locationTypes(boolean activeOnly) {
        return jdbcTemplate.queryForList("""
                SELECT code,
                       name,
                       purpose,
                       available_for_sale,
                       available_for_issue,
                       allow_putaway,
                       allow_picking,
                       allow_reservation,
                       allow_negative_stock,
                       requires_batch,
                       requires_expiry_date,
                       requires_serial_number,
                       requires_quality_release,
                       restricted_access,
                       temporary_location,
                       system_managed,
                       active,
                       display_order,
                       created_at,
                       updated_at
                  FROM storage_location_type
                """ + (activeOnly ? " WHERE active = 1 " : " ")
                + " ORDER BY display_order, name");
    }

    public List<Map<String, Object>> locations(String warehouseId, boolean activeOnly) {
        if (!StringUtils.hasText(warehouseId)) throw new IllegalArgumentException("warehouseId is required");
        return jdbcTemplate.queryForList("""
                SELECT l.id,
                       l.warehouse_id,
                       l.location_code AS code,
                       l.name,
                       l.location_type AS storage_type,
                       t.name AS storage_type_name,
                       t.purpose AS storage_type_purpose,
                       t.available_for_sale,
                       t.available_for_issue,
                       t.allow_putaway,
                       t.allow_picking,
                       t.allow_reservation,
                       t.allow_negative_stock,
                       t.requires_batch,
                       t.requires_expiry_date,
                       t.requires_serial_number,
                       t.requires_quality_release,
                       t.restricted_access,
                       t.temporary_location,
                       t.system_managed,
                       l.description,
                       CASE WHEN UPPER(COALESCE(l.status, 'ACTIVE')) = 'ACTIVE' THEN 1 ELSE 0 END AS active,
                       l.created_at,
                       l.updated_at
                  FROM storage_location l
                  LEFT JOIN storage_location_type t ON t.code = l.location_type
                 WHERE l.warehouse_id = ?
                """ + (activeOnly ? " AND UPPER(COALESCE(l.status, 'ACTIVE')) = 'ACTIVE' " : " ")
                + " ORDER BY l.name", warehouseId);
    }

    public List<Map<String, Object>> bins(String locationId, boolean activeOnly) {
        if (!StringUtils.hasText(locationId)) throw new IllegalArgumentException("locationId is required");
        return jdbcTemplate.queryForList("SELECT * FROM storage_bin_configuration WHERE location_id=? "
                + (activeOnly ? "AND active=1 " : "") + "ORDER BY name", locationId);
    }

    @Transactional
    public Map<String, Object> saveWarehouse(Map<String, Object> body) {
        String id = id(body);
        String status = bool(body.get("active"), true) ? "ACTIVE" : "INACTIVE";
        jdbcTemplate.update("""
            INSERT INTO warehouse(id,warehouse_code,name,description,status,created_at,updated_at)
            VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE warehouse_code=VALUES(warehouse_code),name=VALUES(name),
                                    description=VALUES(description),status=VALUES(status),updated_at=CURRENT_TIMESTAMP
            """, id, required(body, "code").toUpperCase(Locale.ROOT), required(body, "name"), blank(body.get("description")), status);
        return warehouse(id);
    }

    @Transactional
    public Map<String, Object> saveLocationType(Map<String, Object> body) {
        String code = required(body, "code").toUpperCase(Locale.ROOT);
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT system_managed FROM storage_location_type WHERE code=?", code);
        boolean existingSystemManaged = !existing.isEmpty() && bool(existing.get(0).get("system_managed"), false);
        // Only migrations may designate a code as system-managed. Once designated,
        // the flag and active state cannot be cleared through configuration APIs.
        boolean systemManaged = existingSystemManaged;
        boolean active = existingSystemManaged || bool(body.get("active"), true);
        if (!active) {
            Integer assignedLocations = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM storage_location WHERE location_type=?", Integer.class, code);
            if (assignedLocations != null && assignedLocations > 0) {
                throw new IllegalArgumentException(
                        "Storage location type " + code + " cannot be deactivated while it is assigned to a location");
            }
        }

        jdbcTemplate.update("""
            INSERT INTO storage_location_type(
                code,name,purpose,available_for_sale,available_for_issue,allow_putaway,allow_picking,
                allow_reservation,allow_negative_stock,requires_batch,requires_expiry_date,
                requires_serial_number,requires_quality_release,restricted_access,temporary_location,
                system_managed,active,display_order,created_at,updated_at)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                name=VALUES(name),purpose=VALUES(purpose),available_for_sale=VALUES(available_for_sale),
                available_for_issue=VALUES(available_for_issue),allow_putaway=VALUES(allow_putaway),
                allow_picking=VALUES(allow_picking),allow_reservation=VALUES(allow_reservation),
                allow_negative_stock=VALUES(allow_negative_stock),requires_batch=VALUES(requires_batch),
                requires_expiry_date=VALUES(requires_expiry_date),requires_serial_number=VALUES(requires_serial_number),
                requires_quality_release=VALUES(requires_quality_release),restricted_access=VALUES(restricted_access),
                temporary_location=VALUES(temporary_location),system_managed=VALUES(system_managed),
                active=VALUES(active),display_order=VALUES(display_order),updated_at=CURRENT_TIMESTAMP
            """,
                code,
                required(body, "name"),
                blank(body.get("purpose")),
                boolValue(body, "availableForSale", "available_for_sale", false),
                boolValue(body, "availableForIssue", "available_for_issue", false),
                boolValue(body, "allowPutaway", "allow_putaway", false),
                boolValue(body, "allowPicking", "allow_picking", false),
                boolValue(body, "allowReservation", "allow_reservation", false),
                boolValue(body, "allowNegativeStock", "allow_negative_stock", false),
                boolValue(body, "requiresBatch", "requires_batch", false),
                boolValue(body, "requiresExpiryDate", "requires_expiry_date", false),
                boolValue(body, "requiresSerialNumber", "requires_serial_number", false),
                boolValue(body, "requiresQualityRelease", "requires_quality_release", false),
                boolValue(body, "restrictedAccess", "restricted_access", false),
                boolValue(body, "temporaryLocation", "temporary_location", false),
                systemManaged,
                active,
                integer(body.get("displayOrder"), integer(body.get("display_order"), 100))
        );
        return jdbcTemplate.queryForMap("SELECT * FROM storage_location_type WHERE code=?", code);
    }

    @Transactional
    public Map<String, Object> saveLocation(Map<String, Object> body) {
        String id = id(body);
        String warehouseId = required(body, "warehouseId");
        requireWarehouse(warehouseId);
        String status = bool(body.get("active"), true) ? "ACTIVE" : "INACTIVE";
        String locationType = blank(body.get("storageType"));
        if (!StringUtils.hasText(locationType)) locationType = blank(body.get("storage_type"));
        if (!StringUtils.hasText(locationType)) locationType = "GENERAL_STORAGE";
        locationType = requireLocationType(locationType);
        jdbcTemplate.update("""
            INSERT INTO storage_location(id,warehouse_id,location_code,name,location_type,description,status,created_at,updated_at)
            VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id),location_code=VALUES(location_code),
                                    name=VALUES(name),location_type=VALUES(location_type),description=VALUES(description),status=VALUES(status),
                                    updated_at=CURRENT_TIMESTAMP
            """, id, warehouseId, required(body, "code").toUpperCase(Locale.ROOT), required(body, "name"), locationType,
                blank(body.get("description")), status);
        return location(id);
    }

    @Transactional
    public Map<String, Object> saveBin(Map<String, Object> body) {
        String id = id(body);
        String locationId = required(body, "locationId");
        requireLocation(locationId);
        Integer capacity = body.get("capacity") == null || body.get("capacity").toString().isBlank()
                ? null : Integer.valueOf(body.get("capacity").toString());
        jdbcTemplate.update("""
            INSERT INTO storage_bin_configuration(id,location_id,code,name,capacity,description,active)
            VALUES(?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE location_id=VALUES(location_id),code=VALUES(code),name=VALUES(name),capacity=VALUES(capacity),description=VALUES(description),active=VALUES(active)
            """, id, locationId, required(body, "code").toUpperCase(Locale.ROOT), required(body, "name"), capacity, blank(body.get("description")), bool(body.get("active"), true));
        return jdbcTemplate.queryForMap("SELECT * FROM storage_bin_configuration WHERE id=?", id);
    }

    public Map<String, Object> validateSelection(String warehouseId, String locationId, String binId) {
        if (!StringUtils.hasText(warehouseId) || !StringUtils.hasText(locationId) || !StringUtils.hasText(binId)) {
            throw new IllegalArgumentException("Warehouse, storage location and bin are required to complete pickup");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT w.id AS warehouse_id,
                       w.name AS warehouse_name,
                       l.id AS location_id,
                       l.name AS location_name,
                       l.location_type,
                       b.id AS bin_id,
                       b.name AS bin_name
                  FROM warehouse w
                  JOIN storage_location l
                    ON l.warehouse_id = w.id
                   AND UPPER(COALESCE(l.status, 'ACTIVE')) = 'ACTIVE'
                  JOIN storage_location_type t
                    ON t.code = l.location_type
                   AND t.active = 1
                  JOIN storage_bin_configuration b
                    ON b.location_id = l.id
                   AND b.active = 1
                 WHERE w.id = ?
                   AND l.id = ?
                   AND b.id = ?
                   AND UPPER(COALESCE(w.status, 'ACTIVE')) = 'ACTIVE'
                """, warehouseId, locationId, binId);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selected storage warehouse, location and bin are not an active valid hierarchy");
        }
        return rows.get(0);
    }

    private Map<String, Object> warehouse(String id) {
        return jdbcTemplate.queryForMap("""
                SELECT id, warehouse_code AS code, name, description,
                       CASE WHEN UPPER(COALESCE(status, 'ACTIVE')) = 'ACTIVE' THEN 1 ELSE 0 END AS active,
                       created_at, updated_at
                  FROM warehouse WHERE id=?
                """, id);
    }

    private Map<String, Object> location(String id) {
        return jdbcTemplate.queryForMap("""
                SELECT l.id, l.warehouse_id, l.location_code AS code, l.name, l.location_type AS storage_type,
                       t.name AS storage_type_name, t.purpose AS storage_type_purpose,
                       t.available_for_sale, t.available_for_issue, t.allow_putaway, t.allow_picking,
                       t.allow_reservation, t.allow_negative_stock, t.requires_batch, t.requires_expiry_date,
                       t.requires_serial_number, t.requires_quality_release, t.restricted_access,
                       t.temporary_location, t.system_managed,
                       l.description,
                       CASE WHEN UPPER(COALESCE(l.status, 'ACTIVE')) = 'ACTIVE' THEN 1 ELSE 0 END AS active,
                       l.created_at, l.updated_at
                  FROM storage_location l
                  LEFT JOIN storage_location_type t ON t.code = l.location_type
                 WHERE l.id=?
                """, id);
    }

    private void requireWarehouse(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouse WHERE id=?", Integer.class, id);
        if (count == null || count == 0) throw new IllegalArgumentException("Warehouse not found");
    }

    private void requireLocation(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_location WHERE id=?", Integer.class, id);
        if (count == null || count == 0) throw new IllegalArgumentException("Storage location not found");
    }

    private String requireLocationType(String value) {
        String code = value.trim().toUpperCase(Locale.ROOT);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storage_location_type WHERE code=? AND active=1", Integer.class, code);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Unknown or inactive storage location type: " + code);
        }
        return code;
    }

    private String id(Map<String, Object> body) {
        Object value = body.get("id");
        return value == null || value.toString().isBlank() ? UUID.randomUUID().toString() : value.toString();
    }

    private String required(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null && key.endsWith("Id")) {
            String snake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            value = body.get(snake);
        }
        if (value == null || value.toString().trim().isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value.toString().trim();
    }

    private String blank(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private boolean boolValue(Map<String, Object> body, String camel, String snake, boolean fallback) {
        Object value = body.containsKey(camel) ? body.get(camel) : body.get(snake);
        return bool(value, fallback);
    }

    private int integer(Object value, int fallback) {
        if (value == null || value.toString().isBlank()) return fallback;
        return Integer.parseInt(value.toString());
    }

    private boolean bool(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        if (value instanceof byte[] bytes) return bytes.length > 0 && bytes[0] != 0;
        return Objects.equals("true", value.toString().toLowerCase(Locale.ROOT)) || Objects.equals("1", value.toString());
    }
}
