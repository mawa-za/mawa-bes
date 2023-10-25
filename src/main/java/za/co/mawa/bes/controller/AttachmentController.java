package za.co.mawa.bes.controller;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
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
    public ResponseEntity<?> addAttachment(@RequestBody AttachmentCreateDto attachmentCreateDto) {
        try {
            AttachmentDto attachmentDto = attachmentService.save(attachmentCreateDto);
            switch (attachmentCreateDto.getObjectType()){
            case "PARTNER":
                PartnerAttachmentCreateDto partnerAttachmentCreateDto = new PartnerAttachmentCreateDto();
                partnerAttachmentCreateDto.setDocumentType(attachmentCreateDto.getDocumentType());
                partnerAttachmentCreateDto.setPartner(attachmentCreateDto.getObjectId());
                partnerAttachmentCreateDto.setAttachmentId(attachmentDto.getId());
                linkAttachmentPartner(partnerAttachmentCreateDto);
                break;
            case "TRANSACTION":
                TransactionAttachmentCreateDto transactionAttachmentCreateDto = new TransactionAttachmentCreateDto();
                transactionAttachmentCreateDto.setDocumentType(attachmentCreateDto.getDocumentType());
                transactionAttachmentCreateDto.setTransaction(attachmentCreateDto.getObjectId());
                transactionAttachmentCreateDto.setAttachmentId(attachmentDto.getId());
                linkAttachmentTransaction(transactionAttachmentCreateDto);
                break;
        }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/attachment/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttachment(@PathVariable String id) {
        try {
            AttachmentDto attachment = attachmentService.get(id);
            return ResponseEntity.ok(gson.toJson(attachment));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    private void linkAttachmentTransaction(TransactionAttachmentCreateDto transactionAttachmentCreateDto) {
        try {
            TransactionAttachmentPKEntity pkEntity = new TransactionAttachmentPKEntity();
            TransactionAttachmentEntity entity = new TransactionAttachmentEntity();
            pkEntity.setTransaction(transactionAttachmentCreateDto.getTransaction());
            pkEntity.setDocumentType(transactionAttachmentCreateDto.getDocumentType());
            entity.setFileId(transactionAttachmentCreateDto.getAttachmentId());
            entity.setTransactionAttachmentPKEntity(pkEntity);
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate("9999-12-31"));
            transactionService.addAttachment(entity);
        } catch (Exception exception) {

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

    private void linkAttachmentPartner(PartnerAttachmentCreateDto partnerAttachmentCreateDto) {
        try {
            PartnerAttachmentEntity entity = new PartnerAttachmentEntity();
            PartnerAttachmentPKEntity pkEntity = new PartnerAttachmentPKEntity();
            pkEntity.setPartner(partnerAttachmentCreateDto.getPartner());
            pkEntity.setDocumentType(partnerAttachmentCreateDto.getDocumentType());
            entity.setFileId(partnerAttachmentCreateDto.getAttachmentId());
            entity.setPartnerAttachmentPKEntity(pkEntity);
            entity.setStatus(Status.ACTIVE);
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate("9999-12-31"));
            partnerService.addAttachment(entity);
        } catch (Exception exception) {

        }
    }

}
