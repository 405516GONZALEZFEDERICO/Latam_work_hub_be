package Latam.Latam.work.hub.exceptions;

import com.google.firebase.auth.FirebaseAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Error de autenticación");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }



    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<Map<String, String>> handleFirebaseAuthException(FirebaseAuthException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Error de autenticación Firebase");
        errorResponse.put("message", ex.getMessage());

        log.error("Error de Firebase: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Error del servidor");
        errorResponse.put("message", "Se ha producido un error interno");

        log.error("Error no controlado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}