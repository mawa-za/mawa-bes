package za.co.raretag.mawabes.controller;

import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.impl.DefaultClaims;


import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.raretag.mawabes.configuration.jwt.JwtTokenUtil;
import za.co.raretag.mawabes.dto.JwtRequest;
import za.co.raretag.mawabes.dto.JwtResponse;
import za.co.raretag.mawabes.dto.UserDto;
import za.co.raretag.mawabes.dto.UserUpdateDto;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.service.EncryptionService;
import za.co.raretag.mawabes.service.JwtUserDetailsService;
import za.co.raretag.mawabes.service.UserService;


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

    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestHeader HttpHeaders headers, @RequestBody UserDto userDto) throws Exception {

        authenticate(userDto.getId(),userDto.getPassword());
//        userService.authenticate(userDto);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(userDto.getId());

        final String token = jwtTokenUtil.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(token));
    }

//    @RequestMapping(value = "/resetPassword", method = RequestMethod.POST)
//    public ResponseEntity<?> resetPassword(@ @RequestBody UserEntity userEntity) throws Exception {
//        // From the HttpRequest get the claims
//        DefaultClaims claims = (io.jsonwebtoken.impl.DefaultClaims) request.getAttribute("claims");
//        Map<String, Object> expectedMap = getMapFromIoJsonwebtokenClaims(claims);
//        String token = jwtTokenUtil.doGenerateRefreshToken(expectedMap, expectedMap.get("sub").toString());
//        return ResponseEntity.ok(new JwtResponse(token));
//    }

    @RequestMapping(value = "/changePassword", method = RequestMethod.GET)
    public ResponseEntity<?> changePassword(@RequestBody UserUpdateDto userUpdateDto) throws Exception {
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