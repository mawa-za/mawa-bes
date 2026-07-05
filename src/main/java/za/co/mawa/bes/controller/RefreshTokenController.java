package za.co.mawa.bes.controller;

import io.jsonwebtoken.JwtException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.configuration.jwt.JwtRefreshService;
import za.co.mawa.bes.configuration.jwt.JwtResponse;

@RestController
@CrossOrigin
public class RefreshTokenController {
    @Autowired
    JwtRefreshService jwtRefreshService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @RequestMapping(value = "/refresh-token", method = RequestMethod.POST)
    public ResponseEntity<?> tokenRefresh(HttpServletRequest request) throws Exception {

        try {
            String refreshToken = extractRefreshToken(request);

            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Refresh token is required in Authorization Bearer, Refresh-Token header, or refreshToken request body");
            }

            JwtResponse response = jwtRefreshService.refresh(refreshToken);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | JwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ex.getMessage());
        }
    }
    private String extractRefreshToken(HttpServletRequest request) {
        final String refreshHeader = request.getHeader("Refresh-Token");
        if (refreshHeader != null && !refreshHeader.isBlank()) {
            return refreshHeader.trim();
        }

        try {
            String body = request.getReader().lines().reduce("", (a, b) -> a + b);
            if (body != null && !body.isBlank()) {
                JsonNode json = objectMapper.readTree(body);
                for (String key : new String[]{"refreshToken", "refresh_token", "refresh"}) {
                    JsonNode value = json.get(key);
                    if (value != null && !value.asText().isBlank()) {
                        return value.asText().trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }

}
