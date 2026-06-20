package za.co.mawa.bes.controller;


import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PrintJob;
import za.co.mawa.bes.dto.PrintJobRequest;
import za.co.mawa.bes.entity.PrintJobEntity;
import za.co.mawa.bes.repository.PrintJobRepository;

//import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import za.co.mawa.bes.dto.PrintJobCreateRequestDto;
import za.co.mawa.bes.dto.PrintJobResponseDto;
import za.co.mawa.bes.dto.PrintJobUpdateRequestDto;
import za.co.mawa.bes.mapper.PrintJobMapper;


@RestController
@RequestMapping("/print-job")
class PrintJobController {
    Gson gson = new Gson();
    private final Map<Long, PrintJob> jobQueue = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong();
    @Autowired
    PrintJobRepository printJobRepository;

    @PostMapping
    public ResponseEntity<?> createPrintJob(@RequestBody PrintJobRequest request) {
        long id = idCounter.incrementAndGet();
        PrintJob job = new PrintJob(id, request.getPrinterId(), request.getContent(), false);
        jobQueue.put(id, job);
        return ResponseEntity.ok("Job created with ID: " + id);
    }

    @GetMapping
    public ResponseEntity<?> getNextJob(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return printJobRepository.findByCompletedAndPrinterId(false,ipAddress).stream()
                .findFirst()
                .map(job -> ResponseEntity.ok(gson.toJson(job)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> markJobComplete(@PathVariable long id) {
        PrintJobResponseDto job = printJobRepository.getById(id);
        if (job != null) {
            job.setCompleted(true);
            printJobRepository.save(job);
            return ResponseEntity.ok("Marked job " + id + " as complete");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found");
    }
}

