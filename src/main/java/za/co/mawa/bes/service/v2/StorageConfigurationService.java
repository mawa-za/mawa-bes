package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
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

    public List<Map<String, Object>> locations(String warehouseId, boolean activeOnly) {
        if (!StringUtils.hasText(warehouseId)) throw new IllegalArgumentException("warehouseId is required");
        return jdbcTemplate.queryForList("""
                SELECT id,
                       warehouse_id,
                       location_code AS code,
                       name,
                       location_type AS storage_type,
                       description,
                       CASE WHEN UPPER(COALESCE(status, 'ACTIVE')) = 'ACTIVE' THEN 1 ELSE 0 END AS active,
                       created_at,
                       updated_at
                  FROM storage_location
                 WHERE warehouse_id = ?
                """ + (activeOnly ? " AND UPPER(COALESCE(status, 'ACTIVE')) = 'ACTIVE' " : " ")
                + " ORDER BY name", warehouseId);
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
            """, id, required(body, "code"), required(body, "name"), blank(body.get("description")), status);
        return warehouse(id);
    }

    @Transactional
    public Map<String, Object> saveLocation(Map<String, Object> body) {
        String id = id(body);
        String warehouseId = required(body, "warehouseId");
        requireWarehouse(warehouseId);
        String status = bool(body.get("active"), true) ? "ACTIVE" : "INACTIVE";
        String locationType = blank(body.get("storageType"));
        if (!StringUtils.hasText(locationType)) locationType = "BIN";
        jdbcTemplate.update("""
            INSERT INTO storage_location(id,warehouse_id,location_code,name,location_type,description,status,created_at,updated_at)
            VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id),location_code=VALUES(location_code),
                                    name=VALUES(name),location_type=VALUES(location_type),description=VALUES(description),status=VALUES(status),
                                    updated_at=CURRENT_TIMESTAMP
            """, id, warehouseId, required(body, "code"), required(body, "name"), locationType,
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
            """, id, locationId, required(body, "code"), required(body, "name"), capacity, blank(body.get("description")), bool(body.get("active"), true));
        return jdbcTemplate.queryForMap("SELECT * FROM storage_bin_configuration WHERE id=?", id);
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
                SELECT id, warehouse_id, location_code AS code, name, location_type AS storage_type,
                       description,
                       CASE WHEN UPPER(COALESCE(status, 'ACTIVE')) = 'ACTIVE' THEN 1 ELSE 0 END AS active,
                       created_at, updated_at
                  FROM storage_location WHERE id=?
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

    private String id(Map<String, Object> body) {
        Object value = body.get("id");
        return value == null || value.toString().isBlank() ? UUID.randomUUID().toString() : value.toString();
    }

    private String required(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null && key.endsWith("Id")) {
            String snake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            value = body.get(snake);
        }
        if (value == null || value.toString().trim().isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value.toString().trim();
    }

    private String blank(Object value) { return value == null || value.toString().isBlank() ? null : value.toString().trim(); }

    private boolean bool(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return Objects.equals("true", value.toString().toLowerCase()) || Objects.equals("1", value.toString());
    }
}
