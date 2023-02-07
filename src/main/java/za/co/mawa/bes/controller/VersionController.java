package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class VersionController {
    Gson gson = new Gson();
    @RequestMapping(value = "/versions", method = RequestMethod.GET)
    public ResponseEntity<?> getVersions() {
        return ResponseEntity.ok(gson.toJson("{}"));
    }
}
