package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.VersionDto;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.Date;

@RestController
@CrossOrigin
public class VersionController {
    Gson gson = new Gson();
    @RequestMapping(value = "/versions", method = RequestMethod.GET)
    public ResponseEntity<?> getVersions() {
        VersionDto versionDto = new VersionDto();
        versionDto.setVersionNumber("1.0.0");
        versionDto.setAppUsable(true);
        versionDto.setAppName("mawa");
        versionDto.setValidFrom(new Date());
        versionDto.setValidTo(Conversion.stringToDate(Constant.END_DATE));
        return ResponseEntity.ok(gson.toJson(versionDto));
    }
}
