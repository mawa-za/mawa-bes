package za.co.mawa.bes.controller;

// Java POS Printer Server using Spring Boot and ESC/POS

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@RestController
@RequestMapping("/print-receipt")
class PrintController {

    @PostMapping
    public ResponseEntity<String> print(@RequestBody String content) {
        try {
            // Change to your printer's output path
            OutputStream printer = new FileOutputStream("/dev/usb/lp0"); // For Linux
            // OutputStream printer = new FileOutputStream("\\\\localhost\\ReceiptPrinter"); // For Windows

            byte[] escpos = convertToEscPos(content);
            printer.write(escpos);
            printer.flush();
            printer.close();
            return ResponseEntity.ok("Printed successfully");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/network")
    public ResponseEntity<String> printToNetwork(@RequestParam String ip, @RequestParam(defaultValue = "9100") int port, @RequestBody String content) {
        try (Socket socket = new Socket(ip, port);
             OutputStream printer = socket.getOutputStream()) {

            byte[] escpos = convertToEscPos(content);
            printer.write(escpos);
            printer.flush();

            return ResponseEntity.ok("Printed to network printer at " + ip + ":" + port);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Network print failed: " + e.getMessage());
        }
    }

    private byte[] convertToEscPos(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append((char) 27).append('@'); // Initialize printer
        sb.append(content);
        sb.append("\n\n\n");
        sb.append((char) 29).append('V').append((char) 1); // Full cut
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }


}
