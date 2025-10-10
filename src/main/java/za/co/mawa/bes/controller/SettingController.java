package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.SettingInboundDto;
import za.co.mawa.bes.dto.partner.PartnerCreateDto;
import za.co.mawa.bes.service.SettingService;

@RestController
@CrossOrigin
@RequestMapping(value = "setting")
public class SettingController {
    Gson gson = new Gson();
    @Autowired
    SettingService settingService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAll() {
        try {
            String response = gson.toJson(settingService.getSettings());
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@RequestParam String type,
                                 @RequestParam String attribute) {
        try {
            String response = gson.toJson(settingService.getSettingObject(attribute, type));
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(@RequestBody SettingInboundDto settingInboundDto) {
        try {
            settingService.updateSetting(settingInboundDto.getAttribute(), settingInboundDto.getType(), settingInboundDto.getValue());
            String response = gson.toJson(settingService.getSettingObject(settingInboundDto.getAttribute(), settingInboundDto.getType()));
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> put(@RequestBody SettingInboundDto settingInboundDto) {
        try {
            settingService.updateSetting(settingInboundDto.getAttribute(), settingInboundDto.getType(), settingInboundDto.getValue());
            String response = gson.toJson(settingService.getSettingObject(settingInboundDto.getAttribute(), settingInboundDto.getType()));
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

}
