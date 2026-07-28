package za.co.mawa.bes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import za.co.mawa.bes.dto.ErrorResponse;

@RestControllerAdvice
public class MultipartExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(
                        "The selected file is too large. Choose a smaller file and try again.",
                        HttpStatus.PAYLOAD_TOO_LARGE.value()));
    }
}
