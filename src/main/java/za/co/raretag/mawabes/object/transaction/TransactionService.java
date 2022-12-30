package za.co.raretag.mawabes.object.transaction;

import java.util.List;

//@Service
//@Transactional
public class TransactionService {
    private TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionDAO> getTransactions() {

        return null;
    }
}
