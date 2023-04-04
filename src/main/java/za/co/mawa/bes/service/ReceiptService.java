package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.exception.DoesNotExist;

import za.co.mawa.bes.dao.ReceiptDao;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.entity.ReceiptEntity;
import za.co.mawa.bes.utils.NumberRangeType;
import za.co.mawa.bes.repository.ReceiptRepository;
import za.co.mawa.bes.utils.ReceiptType;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;

@Service
public class ReceiptService implements ReceiptDao {

    @Autowired
    ReceiptRepository receiptRepository;
    @Autowired
    NumberRangeService numberRangeService;
    @Override
    public ReceiptDto createReceipt(ReceiptCreateDto receipt) throws Exception {
        try {
           ReceiptEntity entity = new ReceiptEntity();
           entity.setReceiptType(receipt.getReceiptType().toUpperCase());
           entity.setReceiptNumber(numberRangeService.generateNumber(NumberRangeType.RECEIPT));
           if(receipt.getReceiptType().equalsIgnoreCase(ReceiptType.MEMBERSHIP))
           {
               entity.setMembershipNumber(receipt.getMembershipNumber());
               entity.setMembershipPeriod(receipt.getMembershipPeriod());
           }
           entity.setCreationDate(new Date());
           entity.setCreationTime(new Date());
           entity.setCreatedBy(getUser());
           entity.setInvoiceNumber(receipt.getInvoiceNumber());
           entity.setTenderType(receipt.getTenderType());
           entity.setAmount(new BigDecimal(receipt.getAmount()));

           return entityIDtoDto(receiptRepository.save(entity));
        }
        catch (Exception e)
        {
            throw new Exception();
        }
    }

    private ReceiptDto entityIDtoDto(ReceiptEntity entity) throws Exception
    {
        try {
            ReceiptDto receipt = new ReceiptDto();
            receipt.setId(entity.getId());
            return receipt;
        }
        catch (Exception e)
        {
            throw new Exception();
        }

    }

    private String getUser()
    {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }
}
