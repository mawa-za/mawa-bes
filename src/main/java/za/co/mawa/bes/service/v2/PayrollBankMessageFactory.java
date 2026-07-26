package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchEntity;
import za.co.mawa.bes.entity.v2.PayrollPaymentItemEntity;
import za.co.mawa.bes.fnb.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayrollBankMessageFactory {
    private static final String FNB_API = "FNB-API";
    private static final String TENANT = "TENANT";
    private final GcpTenantSecretService secretService;

    public BankPaymentRequest build(
            PayrollPaymentBatchEntity batch,
            List<PayrollPaymentItemEntity> items,
            Map<String, Object> debtorAccount
    ) {
        BankPaymentRequest request = new BankPaymentRequest();
        double controlSum = items.stream().mapToLong(PayrollPaymentItemEntity::getAmountCents).sum() / 100.0;

        GroupHeader header = new GroupHeader();
        header.setMessageId(limit("PAYROLL-" + batch.getId(), 35));
        header.setCreationDateTime(LocalDateTime.now().toString());
        header.setInitiatingPartyName(requiredSetting("COMPANY-NAME", TENANT));
        header.setTotalNumberOfTransactions(items.size());
        header.setTotalControlSum(controlSum);
        request.setGroupHeader(header);

        PaymentInformation payment = new PaymentInformation();
        payment.setPaymentInformationId(limit(batch.getBatchNo(), 35));
        payment.setPaymentInformationMethod("TRF");
        payment.setBatchBooking(true);
        payment.setNumberOfTransactions(items.size());
        payment.setControlSum(controlSum);
        payment.setPaymentTypeInformationServiceLevelCode("SDVA");
        payment.setRequestedExecutionDate(batch.getPaymentDate().toString());

        Debtor debtor = new Debtor();
        debtor.setName(value(debtorAccount, "account_holder"));
        debtor.setBicOrBEI(value(debtorAccount, "branch_code"));
        payment.setDebtor(debtor);

        DebtorAccount debtorBankAccount = new DebtorAccount();
        debtorBankAccount.setAccountNumber(value(debtorAccount, "account_number"));
        debtorBankAccount.setAccountType(toFnbAccountType(value(debtorAccount, "account_type")));
        payment.setDebtorAccount(debtorBankAccount);

        DebtorAgent debtorAgent = new DebtorAgent();
        debtorAgent.setBranchId(value(debtorAccount, "branch_code"));
        payment.setDebtorAgent(debtorAgent);

        String currency = optionalSetting("CURRENCY", TENANT, "ZAR");
        String popRecipient = requiredSetting("POP-RECIPIENT", FNB_API);
        List<CreditTransferTransactionInformation> credits = new ArrayList<>();
        for (PayrollPaymentItemEntity item : items) {
            CreditTransferTransactionInformation credit = new CreditTransferTransactionInformation();
            Amount amount = new Amount();
            amount.setCurrency(currency);
            amount.setValue(BigDecimal.valueOf(item.getAmountCents(), 2).doubleValue());
            credit.setAmount(amount);

            Creditor creditor = new Creditor();
            creditor.setName(item.getAccountHolderName());
            creditor.setBicOrBEI(item.getBranchCode());
            credit.setCreditor(creditor);

            CreditorAccount account = new CreditorAccount();
            account.setAccountNumber(item.getAccountNo());
            account.setAccountType(toFnbAccountType(item.getAccountType()));
            credit.setCreditorAccount(account);

            CreditorAgent agent = new CreditorAgent();
            agent.setBranchId(item.getBranchCode());
            credit.setCreditorAgent(agent);

            credit.setEndToEndId(limit(batch.getBatchNo() + "-" + item.getId(), 30));
            credit.setRemittanceInformationUnstructured(limit(
                    first(item.getPaymentReference(), item.getSalaryReference(), batch.getBatchNo()), 35));
            credit.setRemittanceLocationMethod("EMAL");
            credit.setRemittanceLocationElectronicAddress(popRecipient);
            credits.add(credit);
        }
        payment.setCreditTransferTransactionInformation(credits);
        request.setPaymentInformation(List.of(payment));
        return request;
    }

    private String toFnbAccountType(String value) {
        if (value == null) throw new IllegalArgumentException("Bank account type is required");
        return switch (value.trim().toUpperCase()) {
            case "CHEQUE", "CURRENT", "CACC", "TRANSMISSION", "TRANSACTION", "TRAN" -> "CACC";
            case "SAVINGS", "SAVING", "SVGS" -> "SVGS";
            default -> throw new IllegalArgumentException("Unsupported FNB account type: " + value);
        };
    }

    private String requiredSetting(String key, String group) {
        String value = secretService.resolveSetting(key, group);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required setting: " + group + " / " + key);
        }
        return value;
    }

    private String optionalSetting(String key, String group, String fallback) {
        try {
            String value = secretService.resolveSetting(key, group);
            return value == null || value.isBlank() ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("Payroll debtor account is missing " + key);
        }
        return value.toString();
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "PAYROLL";
    }

    private String limit(String value, int length) {
        if (value == null) return "";
        return value.length() <= length ? value : value.substring(0, length);
    }
}
