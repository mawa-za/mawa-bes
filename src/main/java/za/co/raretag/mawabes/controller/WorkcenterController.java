package za.co.raretag.mawabes.controller;


import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.raretag.mawabes.dto.WorkcenterDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class WorkcenterController {
    Gson gson = new Gson();
    @RequestMapping(value = "/workcenter", method = RequestMethod.GET)
    public ResponseEntity<?> getWorkcenters() {
        List<WorkcenterDto> workcenterDtoList = new ArrayList<>();
        workcenterDtoList.add(new WorkcenterDto("Customer","Customers","Search"));
        workcenterDtoList.add(new WorkcenterDto("Organisation","Organisations","Search"));
        workcenterDtoList.add(new WorkcenterDto("ServiceRequest","Service Requests","Search"));
        workcenterDtoList.add(new WorkcenterDto("LeaveRequest","Leave Requests","Search"));
        workcenterDtoList.add(new WorkcenterDto("LeaveApproval","Leave Approvals","Search"));
        workcenterDtoList.add(new WorkcenterDto("Employee","Employees","Search"));
        return ResponseEntity.ok(gson.toJson(workcenterDtoList));
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
