package za.co.mawa.bes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import za.co.mawa.bes.dto.ErrorResponse;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Converts expected application failures into stable JSON error envelopes.
 *
 * Business and access validation must not be reported as server failures or
 * expose SQL, Hibernate, Java class names or stack traces to frontend users.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);


    @ExceptionHandler({DuplicateCreationException.class, UserExistException.class})
    public ResponseEntity<ErrorResponse> handleDuplicate(Exception ex) {
        return response(HttpStatus.CONFLICT, safeMessage(ex, "This record already exists"));
    }

    @ExceptionHandler({PartnerNotFoundException.class, ProductNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleDomainNotFound(Exception ex) {
        return response(HttpStatus.NOT_FOUND, safeMessage(ex, "The requested information could not be found"));
    }

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedUser(UserLockedException ex) {
        return response(HttpStatus.LOCKED, safeMessage(ex, "This user account is locked"));
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, FileSizeLimitExceededException.class})
    public ResponseEntity<ErrorResponse> handleFileTooLarge(Exception ex) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "The selected file is too large. Choose a smaller file and try again");
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        return response(HttpStatus.FORBIDDEN, safeMessage(ex, "You do not have permission to perform this action"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, safeMessage(ex, "Review the supplied information and try again"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(IllegalStateException ex) {
        return response(HttpStatus.CONFLICT, safeMessage(ex, "The request cannot be completed in the current state"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return response(HttpStatus.NOT_FOUND, safeMessage(ex, "The requested information could not be found"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint rejected a request: {}", rootMessage(ex));
        return response(HttpStatus.CONFLICT, "This record conflicts with existing information. Review the details and try again");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> humanise(error.getField()) + ": " + error.getDefaultMessage())
                .distinct()
                .limit(3)
                .collect(Collectors.joining(". "));
        return response(HttpStatus.BAD_REQUEST,
                message.isBlank() ? "Review the highlighted fields and try again" : message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBinding(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> humanise(error.getField()) + ": " + error.getDefaultMessage())
                .distinct()
                .limit(3)
                .collect(Collectors.joining(". "));
        return response(HttpStatus.BAD_REQUEST,
                message.isBlank() ? "Review the supplied information and try again" : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return response(HttpStatus.BAD_REQUEST, "The request contains invalid or incomplete information");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return response(status, safeText(ex.getReason(), status.getReasonPhrase()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String method = request == null ? "" : request.getMethod();
        String uri = request == null ? "" : request.getRequestURI();
        log.error("Unhandled API failure for {} {}", method, uri, ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "MAWA could not complete the request right now. Please try again shortly");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(withPunctuation(message), status.value()));
    }

    private String safeMessage(Throwable ex, String fallback) {
        return safeText(ex == null ? null : ex.getMessage(), fallback);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String message = value.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
        String lower = message.toLowerCase();
        if (message.length() > 320
                || lower.contains("java.lang.")
                || lower.contains("org.springframework")
                || lower.contains("hibernate")
                || lower.contains("sqlstate")
                || lower.contains("select ")
                || lower.contains("insert ")
                || lower.contains("update ")
                || lower.contains("delete from")
                || lower.contains("stack trace")) {
            return fallback;
        }
        return message;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null || current.getMessage() == null ? throwable.getClass().getSimpleName() : current.getMessage();
    }

    private String humanise(String field) {
        if (field == null || field.isBlank()) return "Field";
        String spaced = field.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String withPunctuation(String message) {
        if (message == null || message.isBlank()) return "Something went wrong.";
        String trimmed = message.trim();
        char last = trimmed.charAt(trimmed.length() - 1);
        return last == '.' || last == '!' || last == '?' ? trimmed : trimmed + ".";
    }
}
