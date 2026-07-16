package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
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
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (UserExistException e) {
            return ResponseEntity.status(HttpStatus.MULTIPLE_CHOICES).body(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUsers(@RequestBody(required = false) UserQueryDto queryDto) {
        try {
            List<UserDto> userDtoList = new ArrayList<>();
            if (queryDto == null) {
                UserQueryDto query = new UserQueryDto();
                userDtoList = userService.getAll(query);
            } else {
                userDtoList = userService.getAll(queryDto);
            }
            return ResponseEntity.ok(gson.toJson(userDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUser(@PathVariable String id) {
        try {
            UserDto userDto = userService.getUserById(id);
            userDto.setPassword(null);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
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

