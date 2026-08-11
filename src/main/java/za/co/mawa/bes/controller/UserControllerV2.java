package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.RoleOutboundDto;
import za.co.mawa.bes.dto.user.*;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.entity.UserRolePKEntity;
import za.co.mawa.bes.exception.UserExistException;
import za.co.mawa.bes.repository.UserRoleRepository;
import za.co.mawa.bes.service.RoleService;
import za.co.mawa.bes.service.UserService;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "/v2/user")
public class UserControllerV2 {
    private static final Logger log = LoggerFactory.getLogger(UserControllerV2.class);
    Gson gson = new Gson();
    @Autowired
    UserService userService;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    RoleService roleService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createUser(@RequestBody UserCreateDto userCreateDto) {
        try {
            UserDto userDto = userService.create(userCreateDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
        } catch (UserExistException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "code", "USER_ALREADY_EXISTS",
                    "message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                    "code", "INVALID_USER_DETAILS",
                    "message", exception.getMessage()));
        } catch (DataIntegrityViolationException exception) {
            log.warn("User creation conflicted with existing data: {}", exception.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "code", "USER_DETAILS_CONFLICT",
                    "message", "A user already exists with one or more of these details"));
        } catch (Exception exception) {
            log.error("Unable to create user", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "code", "USER_CREATION_FAILED",
                    "message", "MAWA could not create the user right now"));
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUsers(@ModelAttribute UserQueryDto queryDto) {
        try {
            // GET filters are query parameters, not a request body. Returning the DTO
            // list directly also avoids serialising the response twice.
            return ResponseEntity.ok(userService.getAll(queryDto));
        } catch (Exception exception) {
            log.error("Unable to load users", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "code", "USER_LIST_LOAD_FAILED",
                    "message", "MAWA could not load the user list right now"));
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUser(@PathVariable String id) {
        try {
            UserDto userDto = userService.getUserById(id);
            userDto.setPassword(null);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (za.co.mawa.bes.exception.DoesNotExist exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of(
                    "message", exception.getMessage() == null ? "User not found" : exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                    "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "message", "MAWA could not load the user details right now"));
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRoles(@PathVariable String id) throws Exception {
        List<RoleOutboundDto> roles = new ArrayList<>();
        List<UserRoleEntity> userRoleEntities = userRoleRepository.findUserRoles(id);
        for(UserRoleEntity userRoleEntity: userRoleEntities){
            roles.add(roleService.get(userRoleEntity.getUserRolePKEntity().getRole()));
        }
        return ResponseEntity.ok(roles);
    }


    @RequestMapping(value = "{id}/role", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRole(@PathVariable String id, @RequestBody List<String> roleList) throws Exception {
        try {
            userService.replaceRoles(id, roleList);
            return ResponseEntity.ok().build();
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of(
                    "code", "PROTECTED_ROLE_ASSIGNMENT_DENIED", "message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "code", exception.getMessage() != null && exception.getMessage().startsWith("LAST_")
                            ? "LAST_TENANT_SUPER_ADMIN" : "PROTECTED_USER",
                    "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}/lock", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> lockUser(@PathVariable String id, @RequestParam("reason") String reason) {
        try {
            return ResponseEntity.ok(gson.toJson(userService.lockuser(id, reason)));
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("code", "PROTECTED_USER_UPDATE_DENIED", "message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("code", "LAST_TENANT_SUPER_ADMIN", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}/unlock", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unlockUser(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(userService.unlockuser(id)));
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("code", "PROTECTED_USER_UPDATE_DENIED", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteRole(@PathVariable String id, @RequestParam("userRole") String userRole) throws Exception {
        try {
            UserRolePKEntity pkEntity = new UserRolePKEntity();
            pkEntity.setRole(userRole);
            pkEntity.setUser(id);
            return ResponseEntity.ok().body(gson.toJson(userService.deleteRole(pkEntity)));
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("code", "PROTECTED_ROLE_ASSIGNMENT_DENIED", "message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("code", "PROTECTED_USER", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteUser(@PathVariable String id) throws Exception {
        try {
            return ResponseEntity.ok().body(gson.toJson(userService.deleteUser(id)));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("code", "PROTECTED_USER", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}/reset", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> restUser(@PathVariable String id) throws Exception {
        try {
            return ResponseEntity.ok().body(gson.toJson(userService.resetUser(id)));
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("code", "PROTECTED_USER_UPDATE_DENIED", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editUser(@PathVariable String id, @RequestBody UserEditDto editDto) throws Exception {
        try {
            return ResponseEntity.ok().body(gson.toJson(userService.editUser(id, editDto)));
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("code", "PROTECTED_USER_UPDATE_DENIED", "message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("code", "PROTECTED_USER", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }


    @RequestMapping(value = "/username/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            UserDto userDto = userService.getUserByName(username);
            userDto.setPassword(null);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            UserDto userDto = userService.getUserByEmail(email);
            userDto.setPassword(null);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "/cellphone/{cellphone}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByCellphone(@PathVariable String cellphone) {
        try {
            UserDto userDto = userService.getUserByCellphone(cellphone);
            userDto.setPassword(null);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "/partner/{partnerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByPartnerId(@PathVariable String partnerId) {
        try {
            UserDto userDto = userService.getUserByPartnerId(partnerId);
            userDto.setPassword(null);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}

