package za.co.mawa.bes.controller;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import za.co.mawa.bes.dto.AttachmentDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.service.AttachmentService;

import java.util.Base64;
import java.util.Date;
import java.time.LocalTime;

import java.util.List;

@RestController
@CrossOrigin
public class AttachmentController {

    Gson gson = new Gson();

    @Autowired
    AttachmentService attachmentService;

    @RequestMapping(value = "/attachment", method = RequestMethod.POST)
    public ResponseEntity<?> addAttachment(@RequestBody AttachmentDto attachmentDto) {
        try {
            AttachmentDto attachmentFile = attachmentService.saveAttachment(attachmentDto);
            return ResponseEntity.ok(gson.toJson(attachmentFile));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/attachmentfile", method = RequestMethod.POST)
    public ResponseEntity<?> fileUpload(@RequestParam("file") MultipartFile multipartFile) {
       // multipartFile.getBytes();
        try {
           // String fileName = multipartFile.getOriginalFilename();
            String fileType = multipartFile.getContentType();
        if(fileType.equalsIgnoreCase("application/pdf") ||fileType.equalsIgnoreCase("image/png") ||
                fileType.equalsIgnoreCase("image/jpeg"))
        {

            byte[] bytesToEncode = multipartFile.getBytes();
            String base64Content = Base64.getEncoder().encodeToString(bytesToEncode);
            AttachmentDto attach = new AttachmentDto();
            attach.setFile(base64Content);
            AttachmentDto attachmentFile = attachmentService.saveAttachment(attach);
            return ResponseEntity.ok(gson.toJson(attachmentFile));
        }

        else {
            return ResponseEntity.badRequest().body(fileType + " file type not supported");
        }

        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(exception);
        }
    }

    @RequestMapping(value = "/attachment/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getAttachment(@PathVariable String id) {
        try {
            AttachmentDto attachment = attachmentService.getAttachment(id);
            return ResponseEntity.ok(gson.toJson(attachment));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
