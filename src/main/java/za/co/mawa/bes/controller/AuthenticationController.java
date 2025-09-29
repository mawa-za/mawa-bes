package za.co.mawa.bes.controller;

import java.util.HashMap;
import java.util.Map;

import com.nimbusds.jose.shaded.gson.Gson;
import io.jsonwebtoken.impl.DefaultClaims;


import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import za.co.mawa.bes.dto.AuthenticationDto;
import za.co.mawa.bes.dto.JwtRequest;
import za.co.mawa.bes.dto.JwtResponse;
import za.co.mawa.bes.service.EncryptionService;
import za.co.mawa.bes.service.JwtUserDetailsService;
import za.co.mawa.bes.service.UserService;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.user.UserUpdateDto;


@RestController
@CrossOrigin
public class AuthenticationController {

    @Autowired
    UserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    EncryptionService encryptionService;
    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Value("${jwt.secret}")
    private String secret;
    Gson gson = new Gson();
    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationDto authenticationDto) throws Exception {
        authenticate(authenticationDto.getUsername(),authenticationDto.getPassword());
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationDto.getUsername());
        final String token = jwtTokenUtil.generateToken(authenticationDto.getUsername());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @RequestMapping(value = "/authenticate-app", method = RequestMethod.POST)
    public ResponseEntity<?> createAppAuthenticationToken(@RequestBody JwtRequest jwtRequest) throws Exception {
        authenticate(jwtRequest.getApplication(),jwtRequest.getPassword());
        final UserDetails userDetails = userDetailsService.loadUserByUsername(jwtRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(jwtRequest.getUsername());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @RequestMapping(value = "/resetPassword", method = RequestMethod.POST)
    public ResponseEntity<?> resetPassword(@RequestBody UserDto userDto) throws Exception {
        userService.reset(userDto);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/changePassword", method = RequestMethod.GET)
    public ResponseEntity<?> changePassword(@RequestBody UserUpdateDto userUpdateDto) throws Exception {
        return ResponseEntity.ok(userService.updatePassword(userUpdateDto));
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.POST)
    public ResponseEntity<?> newPassword(@RequestBody UserUpdateDto userUpdateDto) throws Exception {
        String username = UserContext.getCurrentUser();
        String userID = userService.getUserByName(username).getId();
        userUpdateDto.setId(userID);
        return ResponseEntity.ok(userService.updatePassword(userUpdateDto));
    }

    @RequestMapping(value = "/refreshToken", method = RequestMethod.GET)
    public ResponseEntity<?> refreshtoken(HttpServletRequest request) throws Exception {
        // From the HttpRequest get the claims
        DefaultClaims claims = (io.jsonwebtoken.impl.DefaultClaims) request.getAttribute("claims");
        Map<String, Object> expectedMap = getMapFromIoJsonwebtokenClaims(claims);
        String token = jwtTokenUtil.doGenerateRefreshToken(expectedMap, expectedMap.get("sub").toString());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    public Map<String, Object> getMapFromIoJsonwebtokenClaims(DefaultClaims claims) {
        Map<String, Object> expectedMap = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            expectedMap.put(entry.getKey(), entry.getValue());
        }
        return expectedMap;
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}