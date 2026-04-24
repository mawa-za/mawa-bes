package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReceiptSyncResponse implements Serializable {

    private String status;        // SUCCESS | DUPLICATE | FAILED | RETRY
    private String receiptId;
    private String receiptNumber;

    private String errorCode;
    private String message;

}