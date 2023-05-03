package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "transaction_bank_account")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionBankAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(unique = true, name = "transaction")
    private String transaction;
    @Column(name = "account_holder")
    private String accountHolder;
    @Column(name = "bank_name")
    private String bankName;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "branch_code")
    private String branchCode;
    @Column(name = "account_type")
    private String accountType;

}
