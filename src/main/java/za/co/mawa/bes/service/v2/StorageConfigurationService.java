package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageConfigurationService {
    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> warehouses(boolean activeOnly) {
        return jdbcTemplate.queryForList("SELECT * FROM storage_warehouse "
                + (activeOnly ? "WHERE active=1 " : "") + "ORDER BY name");
    }

    public List<Map<String, Object>> locations(String warehouseId, boolean activeOnly) {
        if (!StringUtils.hasText(warehouseId)) throw new IllegalArgumentException("warehouseId is required");
        return jdbcTemplate.queryForList("SELECT * FROM storage_location WHERE warehouse_id=? "
                + (activeOnly ? "AND active=1 " : "") + "ORDER BY name", warehouseId);
    }

    public List<Map<String, Object>> bins(String locationId, boolean activeOnly) {
        if (!StringUtils.hasText(locationId)) throw new IllegalArgumentException("locationId is required");
        return jdbcTemplate.queryForList("SELECT * FROM storage_bin_configuration WHERE location_id=? "
                + (activeOnly ? "AND active=1 " : "") + "ORDER BY name", locationId);
    }

    @Transactional
    public Map<String, Object> saveWarehouse(Map<String, Object> body) {
        String id = id(body);
        jdbcTemplate.update("""
            INSERT INTO storage_warehouse(id,code,name,description,active)
            VALUES(?,?,?,?,?)
            ON DUPLICATE KEY UPDATE code=VALUES(code),name=VALUES(name),description=VALUES(description),active=VALUES(active)
            """, id, required(body, "code"), required(body, "name"), blank(body.get("description")), bool(body.get("active"), true));
        return jdbcTemplate.queryForMap("SELECT * FROM storage_warehouse WHERE id=?", id);
    }

    @Transactional
    public Map<String, Object> saveLocation(Map<String, Object> body) {
        String id = id(body);
        String warehouseId = required(body, "warehouseId");
        requireWarehouse(warehouseId);
        jdbcTemplate.update("""
            INSERT INTO storage_location(id,warehouse_id,code,name,storage_type,description,active)
            VALUES(?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id),code=VALUES(code),name=VALUES(name),storage_type=VALUES(storage_type),description=VALUES(description),active=VALUES(active)
            """, id, warehouseId, required(body, "code"), required(body, "name"), blank(body.get("storageType")), blank(body.get("description")), bool(body.get("active"), true));
        return jdbcTemplate.queryForMap("SELECT * FROM storage_location WHERE id=?", id);
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

    public Map<String, Object> validateSelection(String warehouseId, String locationId, String binId) {
        if (!StringUtils.hasText(warehouseId) || !StringUtils.hasText(locationId) || !StringUtils.hasText(binId)) {
            throw new IllegalArgumentException("Warehouse, storage location and bin are required to complete pickup");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT w.id warehouse_id,w.name warehouse_name,l.id location_id,l.name location_name,
                   b.id bin_id,b.name bin_name
              FROM storage_warehouse w
              JOIN storage_location l ON l.warehouse_id=w.id AND l.active=1
              JOIN storage_bin_configuration b ON b.location_id=l.id AND b.active=1
             WHERE w.id=? AND l.id=? AND b.id=? AND w.active=1
            """, warehouseId, locationId, binId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Selected storage warehouse, location and bin are not an active valid hierarchy");
        return rows.get(0);
    }

    private void requireWarehouse(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_warehouse WHERE id=?", Integer.class, id);
        if (count == null || count == 0) throw new IllegalArgumentException("Storage warehouse not found: " + id);
    }

    private void requireLocation(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_location WHERE id=?", Integer.class, id);
        if (count == null || count == 0) throw new IllegalArgumentException("Storage location not found: " + id);
    }

    private static String id(Map<String, Object> body) {
        String id = Objects.toString(body.get("id"), "").trim();
        return id.isEmpty() ? UUID.randomUUID().toString() : id;
    }
    private static String required(Map<String, Object> body, String key) {
        String value = blank(body.get(key));
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(key + " is required");
        return value;
    }
    private static String blank(Object value) { return value == null ? null : value.toString().trim(); }
    private static boolean bool(Object value, boolean fallback) { return value == null ? fallback : Boolean.parseBoolean(value.toString()); }
}
