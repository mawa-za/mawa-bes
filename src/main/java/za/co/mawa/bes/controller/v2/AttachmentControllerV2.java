package za.co.mawa.bes.controller.v2;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.TransactionService;

@RestController
@CrossOrigin
@RequestMapping(value = "v2/attachment")
public class AttachmentControllerV2 {

    Gson gson = new Gson();

    @Autowired
    AttachmentService attachmentService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    TransactionService transactionService;

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addAttachment(@RequestBody AttachmentCreateDto attachmentCreateDto) {
        try {
            attachmentService.save(attachmentCreateDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET,produces = MediaType.MULTIPART_MIXED_VALUE)
    public ResponseEntity<?> getAttachment(@PathVariable String id) {
        try {
            String file = attachmentService.get(id);
            return ResponseEntity.ok(file);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteAttachment(@PathVariable String id) {
        try {
             attachmentService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttachments(@RequestParam(required = true) String objectId) {
        try {
            return ResponseEntity.ok(gson.toJson(attachmentService.getAll(objectId)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }


}
