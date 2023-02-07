package za.co.mawa.bes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class VersionController {
    @RequestMapping(value = "/versions", method = RequestMethod.GET)
    public ResponseEntity<?> getVersions() {
        return ResponseEntity.ok().build();
    }
}
