package za.co.mawa.bes.controller;

import io.jsonwebtoken.JwtException;
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
    @RequestMapping(value = "/refresh-token", method = RequestMethod.POST)
    public ResponseEntity<?> tokenRefresh(HttpServletRequest request) throws Exception {

        try {
            String refreshToken = "";
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                refreshToken = authHeader.substring(7);
            }
//            String refreshToken = request.getHeader("Refresh-Token");

            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Refresh-Token header is required");
            }

            JwtResponse response = jwtRefreshService.refresh(refreshToken);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | JwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ex.getMessage());
        }
    }
}
