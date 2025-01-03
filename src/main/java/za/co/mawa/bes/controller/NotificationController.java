package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.notification.NotificationCreate1Dto;
import za.co.mawa.bes.dto.notification.NotificationCreateDto;
import za.co.mawa.bes.dto.notification.NotificationDto;
import za.co.mawa.bes.service.NotificationService;

import java.util.*;
@RestController
@CrossOrigin
@RequestMapping(value = "notifications")
public class NotificationController {
    @Autowired
    NotificationService notificationService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postNotification(@RequestBody NotificationCreate1Dto notificationDto) {
        try {
            return ResponseEntity.ok(gson.toJson(notificationService.create(notificationDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNotification(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(notificationService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/byTrans", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNotifications(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(notificationService.notifications(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/read", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateStatus(@PathVariable String id) {
        try {
//
            notificationService.read(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/partner", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerNotifications(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(notificationService.getPartnerNotifications(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNotifications() {
        try {
            return ResponseEntity.ok(gson.toJson(notificationService.getAllNotifications()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
