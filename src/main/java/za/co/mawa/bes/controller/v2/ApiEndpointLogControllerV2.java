package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.ApiEndpointLogResponseDto;
import za.co.mawa.bes.service.v2.ApiEndpointLogQueryService;

@CrossOrigin
@RestController
@RequestMapping("v2/api-endpoint-logs")
@RequiredArgsConstructor
public class ApiEndpointLogControllerV2 {

    private final ApiEndpointLogQueryService service;

    @GetMapping
    public Page<ApiEndpointLogResponseDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }
}