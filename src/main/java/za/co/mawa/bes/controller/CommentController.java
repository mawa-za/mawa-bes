package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.comment.CommentInboundDto;
import za.co.mawa.bes.service.CommentService;

@RestController
@RequestMapping(value = "comment")
public class CommentController {

    @Autowired
    CommentService commentService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postComment(@RequestBody CommentInboundDto commentInboundDto) {
        try {
            return ResponseEntity.ok(gson.toJson(commentService.create(commentInboundDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getComment(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(commentService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping( method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllComment(@RequestParam String parentTransactionId,
                                           @RequestParam String createdBy) {
        try {

            return ResponseEntity.ok(gson.toJson(commentService.getAll(parentTransactionId)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editComment(@PathVariable String id,
                                         @RequestBody CommentInboundDto commentInboundDto) {
        try {
            return ResponseEntity.ok(gson.toJson(commentService.edit(commentInboundDto,id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteComment(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(commentService.delete(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
