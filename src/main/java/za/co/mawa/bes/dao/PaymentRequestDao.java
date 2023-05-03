package za.co.mawa.bes.dao;
import za.co.mawa.bes.dto.payment.request.PaymentRequestCreateDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.exception.DoesNotExist;
public interface PaymentRequestDao {
    String create(PaymentRequestCreateDto paymentRequest) throws Exception;
    PaymentRequestDto get(String id) throws DoesNotExist,Exception;

}
