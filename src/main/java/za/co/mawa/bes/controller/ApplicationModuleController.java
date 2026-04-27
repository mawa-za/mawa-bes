package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.ApplicationModuleDto;
import za.co.mawa.bes.service.ApplicationModuleService;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
public class ApplicationModuleController {

    @Autowired
    private ApplicationModuleService applicationModuleService;

    @GetMapping
    public List<ApplicationModuleDto> getAllModules() {
        return applicationModuleService.getAllModules();
    }
}