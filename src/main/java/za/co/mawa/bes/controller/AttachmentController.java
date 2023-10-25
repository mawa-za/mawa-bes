package za.co.mawa.bes.controller;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import za.co.mawa.bes.dto.attachment.AttachmentDto;
import za.co.mawa.bes.dto.PartnerAttachmentCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionAttachmentCreateDto;
import za.co.mawa.bes.entity.transaction.TransactionAttachmentEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttachmentPKEntity;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.entity.PartnerAttachmentEntity;
import za.co.mawa.bes.entity.PartnerAttachmentPKEntity;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.Conversion;

import java.util.Base64;
import java.util.Date;

@RestController
@CrossOrigin
public class AttachmentController {

    Gson gson = new Gson();

    @Autowired
    AttachmentService attachmentService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    TransactionService transactionService;

    @RequestMapping(value = "/attachment", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addAttachment(@RequestBody AttachmentDto attachmentDto) {
        try {
            AttachmentDto attachmentFile = attachmentService.saveAttachment(attachmentDto);
            return ResponseEntity.ok(gson.toJson(attachmentFile));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/attachmentfile", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "/attachment/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttachment(@PathVariable String id) {
        try {
            AttachmentDto attachment = attachmentService.getAttachment(id);
            return ResponseEntity.ok(gson.toJson(attachment));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/attachment/{id}/transaction", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> linkAttachmentTransaction(@PathVariable String id,@RequestBody TransactionAttachmentCreateDto transactionAttachment) {
        try {
            TransactionAttachmentPKEntity pkEntity = new TransactionAttachmentPKEntity();
            TransactionAttachmentEntity entity = new TransactionAttachmentEntity();
            pkEntity.setTransaction(transactionAttachment.getTransaction());
            pkEntity.setFileType(transactionAttachment.getFileType());
            entity.setFileId(id);
            entity.setTransactionAttachmentPKEntity(pkEntity);
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate("9999-12-31"));
            boolean added = transactionService.addAttachment(entity);
            return ResponseEntity.ok(gson.toJson(added));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/attachment/transaction/{transactionId}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttachmentTransaction(@PathVariable String transactionId) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.getAttachments(transactionId)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/attachment/transaction/{transactionId}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeAttachmentTransaction(@PathVariable String transactionId,@RequestParam String fileType) {
        try {
            TransactionAttachmentPKEntity entityPk = new TransactionAttachmentPKEntity();
            entityPk.setTransaction(transactionId);
            entityPk.setFileType(fileType);
            return ResponseEntity.ok(gson.toJson(transactionService.removeAttachment(entityPk)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/attachment/{id}/partner", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> linkAttachmentPartner(@PathVariable String id,@RequestBody PartnerAttachmentCreateDto partnerAttachment) {
        try {
            PartnerAttachmentEntity entity = new PartnerAttachmentEntity();
            PartnerAttachmentPKEntity pkEntity = new PartnerAttachmentPKEntity();
            pkEntity.setPartner(partnerAttachment.getPartner());
            pkEntity.setFileType(partnerAttachment.getFileType());
            entity.setFileId(id);
            entity.setPartnerAttachmentPKEntity(pkEntity);
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate("9999-12-31"));
            boolean added = partnerService.addAttachment(entity);
            return ResponseEntity.ok(gson.toJson(added));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

}
