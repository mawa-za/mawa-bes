package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.SampleCreateRequestDto;
import za.co.mawa.bes.dto.v2.SampleResponseDto;
import za.co.mawa.bes.dto.v2.SampleUpdateRequestDto;
import za.co.mawa.bes.entity.v2.SampleEntity;
import za.co.mawa.bes.mapper.v2.SampleMapper;
import za.co.mawa.bes.service.v2.SampleService;

import java.util.List;

@RestController
@RequestMapping("/v2/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;
    private final SampleMapper sampleMapper;

    @GetMapping
    public ResponseEntity<List<SampleResponseDto>> findAll() {
        return ResponseEntity.ok(sampleService.findAll().stream()
                .map(sampleMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SampleResponseDto> findById(@PathVariable String id) {
        return ResponseEntity.ok(sampleMapper.toResponse(sampleService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<SampleResponseDto> create(@RequestBody SampleCreateRequestDto request) {
        SampleEntity saved = sampleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sampleMapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SampleResponseDto> update(@PathVariable String id,
                                                    @RequestBody SampleUpdateRequestDto request) {
        SampleEntity saved = sampleService.update(id, request);
        return ResponseEntity.ok(sampleMapper.toResponse(saved));
    }
}
