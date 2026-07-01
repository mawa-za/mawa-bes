package za.co.mawa.bes.dto.v2.payapp;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CashupRequest {

    private Long cashupNo;

    private String deviceId;
    private String userId;
    private String tenantId;

    /**
     * ISO date or datetime from app.
     * Example:
     * 2026-05-12
     * 2026-05-12T08:15:00Z
     */
    private String date;

    /**
     * OPEN while the device is actively receipting.
     * SUBMITTED when the cashier closes/submits the cashup.
     * Older Flutter payloads may omit this; the service will infer from notes where possible.
     */
    private String status;

    private Long totalCents;
    private Integer receiptCount;

    /**
     * Example:
     * {
     *   "CASH": 120000,
     *   "CARD": 50000
     * }
     */
    private Map<String, Long> amountByMethod;

    /**
     * Example:
     * {
     *   "CASH": 5,
     *   "CARD": 2
     * }
     */
    private Map<String, Integer> countByMethod;

    private List<CashupReceiptRequest> receipts;

    private String notes;
}