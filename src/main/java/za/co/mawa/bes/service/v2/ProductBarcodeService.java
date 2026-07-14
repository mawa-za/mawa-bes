package za.co.mawa.bes.service.v2;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductBarcodeService {
    private final JdbcTemplate jdbcTemplate;

    public ProductBarcodeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(String productId) {
        requireProduct(productId);
        return jdbcTemplate.queryForList(
                "SELECT id, product_id, barcode, barcode_type, is_primary, created_at, created_by " +
                        "FROM product_barcode WHERE product_id = ? ORDER BY is_primary DESC, created_at ASC",
                productId
        );
    }

    public Map<String, Object> findProduct(String barcode) {
        String normalized = normalize(barcode);
        return jdbcTemplate.queryForMap(
                "SELECT p.id, p.code, p.description, p.type, p.uom, pb.barcode, pb.barcode_type, pb.is_primary " +
                        "FROM product_barcode pb JOIN product p ON p.id = pb.product_id WHERE pb.barcode = ?",
                normalized
        );
    }

    @Transactional
    public List<Map<String, Object>> replace(String productId, BarcodeReplaceRequest request, String userId) {
        requireProduct(productId);
        Set<String> unique = new LinkedHashSet<>();
        if (request != null && request.barcodes() != null) {
            for (String value : request.barcodes()) {
                String normalized = normalize(value);
                if (!normalized.isEmpty()) unique.add(normalized);
            }
        }
        jdbcTemplate.update("DELETE FROM product_barcode WHERE product_id = ?", productId);
        boolean primary = true;
        for (String barcode : unique) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO product_barcode (id, product_id, barcode, barcode_type, is_primary, created_at, created_by) VALUES (?,?,?,?,?,?,?)",
                        UUID.randomUUID().toString(), productId, barcode,
                        request == null || request.barcodeType() == null || request.barcodeType().isBlank()
                                ? "EAN" : request.barcodeType().trim().toUpperCase(),
                        primary, Timestamp.valueOf(LocalDateTime.now()), userId
                );
            } catch (DuplicateKeyException duplicate) {
                throw new IllegalArgumentException("Barcode " + barcode + " is already assigned to another product");
            }
            primary = false;
        }
        return list(productId);
    }

    @Transactional
    public List<Map<String, Object>> add(String productId, BarcodeRequest request, String userId) {
        requireProduct(productId);
        String barcode = normalize(request == null ? null : request.barcode());
        if (barcode.isEmpty()) throw new IllegalArgumentException("Barcode is required");
        boolean makePrimary = request.primary() || jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_barcode WHERE product_id = ?", Integer.class, productId) == 0;
        if (makePrimary) jdbcTemplate.update("UPDATE product_barcode SET is_primary = 0 WHERE product_id = ?", productId);
        try {
            jdbcTemplate.update(
                    "INSERT INTO product_barcode (id, product_id, barcode, barcode_type, is_primary, created_at, created_by) VALUES (?,?,?,?,?,?,?)",
                    UUID.randomUUID().toString(), productId, barcode,
                    request.barcodeType() == null || request.barcodeType().isBlank() ? "EAN" : request.barcodeType().trim().toUpperCase(),
                    makePrimary, Timestamp.valueOf(LocalDateTime.now()), userId
            );
        } catch (DuplicateKeyException duplicate) {
            throw new IllegalArgumentException("Barcode " + barcode + " is already assigned to another product");
        }
        return list(productId);
    }

    @Transactional
    public void delete(String productId, String barcodeId) {
        int deleted = jdbcTemplate.update("DELETE FROM product_barcode WHERE id = ? AND product_id = ?", barcodeId, productId);
        if (deleted == 0) throw new IllegalArgumentException("Barcode not found");
        Integer primaryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_barcode WHERE product_id = ? AND is_primary = 1", Integer.class, productId);
        if (primaryCount != null && primaryCount == 0) {
            List<String> ids = jdbcTemplate.queryForList(
                    "SELECT id FROM product_barcode WHERE product_id = ? ORDER BY created_at ASC LIMIT 1", String.class, productId);
            if (!ids.isEmpty()) jdbcTemplate.update("UPDATE product_barcode SET is_primary = 1 WHERE id = ?", ids.get(0));
        }
    }

    private void requireProduct(String productId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product WHERE id = ?", Integer.class, productId);
        if (count == null || count == 0) throw new IllegalArgumentException("Product not found: " + productId);
    }

    private String normalize(String barcode) {
        return barcode == null ? "" : barcode.trim().replaceAll("\\s+", "").toUpperCase();
    }

    public record BarcodeRequest(String barcode, String barcodeType, boolean primary) {}
    public record BarcodeReplaceRequest(List<String> barcodes, String barcodeType) {
        public BarcodeReplaceRequest {
            if (barcodes == null) barcodes = new ArrayList<>();
        }
    }
}
