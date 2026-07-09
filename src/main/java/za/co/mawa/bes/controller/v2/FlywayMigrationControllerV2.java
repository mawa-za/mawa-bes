package za.co.mawa.bes.controller.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.configuration.flyway.FlywayConfiguration;

import java.util.Map;

@RestController
@RequestMapping("/v2/flyway")
public class FlywayMigrationControllerV2 {

    @Autowired
    private FlywayConfiguration flywayConfiguration;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(flywayConfiguration.getMigrationStatus());
    }

    @PostMapping("/run-now")
    public ResponseEntity<Object> runNow(@RequestParam(name = "blocking", defaultValue = "false") boolean blocking) {
        if (blocking) {
            return ResponseEntity.ok(flywayConfiguration.runMigrationsBlocking("manual-api"));
        }
        return ResponseEntity.accepted().body(flywayConfiguration.runMigrationsAsync("manual-api"));
    }
}
