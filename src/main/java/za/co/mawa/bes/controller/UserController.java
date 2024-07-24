package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.user.*;
import za.co.mawa.bes.entity.UserRolePKEntity;
import za.co.mawa.bes.exception.UserExistException;
import za.co.mawa.bes.service.UserService;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "user")
public class UserController {
    Gson gson = new Gson();
    @Autowired
    UserService userService;

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
        return ResponseEntity.ok(userService.getRoles(id));
    }
    @RequestMapping(value = "{id}/role", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRole(@PathVariable String id, @RequestBody List<String> roleList) throws Exception {
        try {
            for (String role : roleList) {
                UserRoleDto userRoleDto = new UserRoleDto();
                userRoleDto.setUser(id);
                userRoleDto.setRole(role);
                userService.addRole(userRoleDto);
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/{id}/lock", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> lockUser(@PathVariable String id, @RequestParam("reason") String reason) {
        try {
            return ResponseEntity.ok(gson.toJson(userService.lockuser(id, reason)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/unlock", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unlockUser(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(userService.unlockuser(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteRole(@PathVariable String id, @RequestParam("userRole") String userRole) throws Exception {
        try {
            UserRolePKEntity pkEntity = new UserRolePKEntity();
            pkEntity.setRole(userRole);
            pkEntity.setUser(id);
            return ResponseEntity.ok().body(gson.toJson(userService.deleteRole(pkEntity)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteUser(@PathVariable String id) throws Exception {
        try {
            return ResponseEntity.ok().body(gson.toJson(userService.deleteUser(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/reset", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> restUser(@PathVariable String id) throws Exception {
        try {
            return ResponseEntity.ok().body(gson.toJson(userService.resetUser(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editUser(@PathVariable String id, @RequestBody UserEditDto editDto) throws Exception {
        try {
            return ResponseEntity.ok().body(gson.toJson(userService.editUser(id, editDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
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
