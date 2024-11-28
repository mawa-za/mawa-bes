package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.utils.DateTime;
@NoArgsConstructor
@Getter
@Setter
public class BankFileXmlDto {


private String creationDateTime;

private String messageIdentification;

private Integer numberOfTransactions;

private Double controlSum;

private String name;

private String paymentInformationIdentification;
private String paymentMethod;
private Boolean batchBooking;
private String code;
private String requestedExecutionDate;
private String identification;
private String proprietary;
private String memberId;
private String endToEndIdentification;
private Double amount;
private Double instructedAmount;
private String notes;
private String unStructured;





}
