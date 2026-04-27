package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.ApplicationModuleDto;

import java.util.Arrays;
import java.util.List;

@Service
public class ApplicationModuleService {

    public List<ApplicationModuleDto> getAllModules() {
        // Mocked list of modules
        return Arrays.asList(
            new ApplicationModuleDto("Module1","Module1", "Description of Module1"),
            new ApplicationModuleDto("Module2","Module2", "Description of Module2"),
            new ApplicationModuleDto("Module3","Module3", "Description of Module3")
        );
    }
}