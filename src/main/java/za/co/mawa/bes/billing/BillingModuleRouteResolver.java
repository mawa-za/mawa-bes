package za.co.mawa.bes.billing;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class BillingModuleRouteResolver {

    private static final List<RouteModule> ROUTES = List.of(
            route("MEMBERSHIP",
                    "/membership", "/v1/membership", "/v2/membership", "/v2/memberships",
                    "/premium", "/v2/premium", "/v2/premium-generation",
                    "/group-society", "/v2/group-society"),
            route("CLAIMS", "/claim", "/claimbymember", "/v2/claim", "/v2/claims", "/v2/membership-claim"),
            route("FUNERAL",
                    "/funeral", "/v2/funeral", "/v2/funeral-arrangements", "/v2/funeral-underwriting",
                    "/mortuary", "/v2/mortuary", "/pickup-request", "/v2/pickup", "/service-request"),
            route("TOMBSTONES", "/tombstone", "/v2/tombstone", "/v2/tombstones"),
            route("INVENTORY",
                    "/product", "/v2/product", "/purchase-order", "/quotation", "/sales-order",
                    "/goods-receipt", "/putaway", "/stock", "/storage-bin", "/product-storage-bin"),
            route("HR",
                    "/employment", "/v2/employment", "/leave-request", "/v2/leave", "/v2/leave-request",
                    "/payroll", "/v2/payroll-payment-batch", "/employees", "/v2/assets"),
            route("ACCOUNTING",
                    "/invoice", "/v2/invoice", "/cashup", "/cashup-range", "/v2/cashup", "/deposit", "/deposit-attachment", "/xero",
                    "/v2/integrations/xero", "/v2/integrations/fnb", "/accounting", "/ledger", "/journal", "/voucher"),
            route("PAYMENTS",
                    "/payment-request", "/v2/payment-request", "/bank-account", "/bank-file", "/receipt-range",
                    "/v2/manual-receipts", "/v2/receipts", "/v2/payment-batches", "/v2/pay-app", "/pay-app",
                    "/v2/payment-account-configurations"),
            route("LEGAL", "/case", "/v2/case", "/v2/cases", "/legal", "/matter", "/time-entry", "/disbursement"),
            route("CALENDAR", "/calendar", "/appointment", "/v2/appointment"),
            route("DOCUMENTS", "/attachment", "/v2/attachment", "/document", "/v2/document"),
            route("POS", "/print-job", "/print-receipt", "/v2/pos", "/v2/pos-printing", "/v2/pos-print-agents"),
            route("REPORTING", "/report", "/reports", "/reporting")
    );
    public String resolve(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return null;
        }
        String normalized = normalizePath(requestUri);
        return ROUTES.stream()
                .filter(route -> route.prefixes().stream().anyMatch(prefix -> matchesPrefix(normalized, prefix)))
                .map(RouteModule::moduleCode)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private String normalizePath(String requestUri) {
        String path = requestUri.split("\\?", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static RouteModule route(String moduleCode, String... prefixes) {
        return new RouteModule(moduleCode, List.of(prefixes));
    }

    private record RouteModule(String moduleCode, List<String> prefixes) {
    }
}
