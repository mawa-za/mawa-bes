package za.co.mawa.bes.controller;


import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.service.WorkcenterService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class WorkcenterController {
    @Autowired
    WorkcenterService workcenterService;
    Gson gson = new Gson();
    @RequestMapping(value = "/workcenter", method = RequestMethod.GET)
    public ResponseEntity<?> getWorkcenters() {
        return ResponseEntity.ok(gson.toJson(workcenterService.getAll()));
    }

    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
