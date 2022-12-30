package za.co.raretag.mawabes.object.transaction;

public interface TransactionDAO {
    String insert(TransactionDTO transactionDTO);
    String update(TransactionDTO transactionDTO);
    String delete(String id);
    TransactionDTO findById(String id);
}
