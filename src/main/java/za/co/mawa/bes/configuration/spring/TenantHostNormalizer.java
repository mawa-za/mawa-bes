package za.co.mawa.bes.configuration.spring;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Normalises a tenant reference supplied as a host, URL, Origin/Referer value,
 * or forwarded host into the canonical hostname stored against the tenant.
 */
public final class TenantHostNormalizer {

    private TenantHostNormalizer() {
    }

    public static String normalize(String value) {
        if (!hasText(value)) {
            return null;
        }

        String candidate = value.trim();

        // Forwarded headers may contain a comma-separated list. The first value
        // represents the original client-facing host.
        int commaIndex = candidate.indexOf(',');
        if (commaIndex >= 0) {
            candidate = candidate.substring(0, commaIndex).trim();
        }

        candidate = stripMatchingQuotes(candidate);
        if (!hasText(candidate)) {
            return null;
        }

        String host = parseHost(candidate);
        if (!hasText(host)) {
            host = fallbackHost(candidate);
        }
        if (!hasText(host)) {
            return null;
        }

        host = host.trim().toLowerCase(Locale.ROOT);
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return hasText(host) ? host : null;
    }

    private static String parseHost(String candidate) {
        try {
            URI uri = hasScheme(candidate)
                    ? new URI(candidate)
                    : new URI("https://" + candidate);
            return uri.getHost();
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean hasScheme(String candidate) {
        int separator = candidate.indexOf("://");
        return separator > 0;
    }

    private static String fallbackHost(String candidate) {
        String value = candidate;
        int schemeIndex = value.indexOf("://");
        if (schemeIndex >= 0) {
            value = value.substring(schemeIndex + 3);
        }

        int terminator = firstPositiveIndex(
                value.indexOf('/'),
                value.indexOf('?'),
                value.indexOf('#')
        );
        if (terminator >= 0) {
            value = value.substring(0, terminator);
        }

        int userInfoIndex = value.lastIndexOf('@');
        if (userInfoIndex >= 0) {
            value = value.substring(userInfoIndex + 1);
        }

        // A tenant hostname may include a port. Keep bracketed IPv6 values intact.
        if (!value.startsWith("[")) {
            int colonIndex = value.indexOf(':');
            if (colonIndex >= 0) {
                value = value.substring(0, colonIndex);
            }
        }

        return value;
    }

    private static int firstPositiveIndex(int... indexes) {
        int result = -1;
        for (int index : indexes) {
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String stripMatchingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }
}
