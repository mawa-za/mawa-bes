package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class MembershipService implements MembershipDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;

    @Override
    public MembershipDto create(MembershipCreateDto membershipCreateDto) throws PartnerNotFoundException, ProductNotFoundException, TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {

        if (partnerService.get(membershipCreateDto.getMemberId()) == null) {
            throw new PartnerNotFoundException("Membership main member does not exist");
        }
        if (productService.get(membershipCreateDto.getProductId()) == null) {
            throw new ProductNotFoundException("Membership product does not exist");
        }
//        if (partnerService.get(membershipCreateDto.getSalesRepresentativeId()) == null) {
//            throw new PartnerNotFoundException("Membership Sales Representative does not exist");
//        }
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.MEMBERSHIP);
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);

        if (membershipCreateDto.getProductId() != null) {
            ProductDto productDto = productService.get(membershipCreateDto.getProductId());
            TransactionItemDto transactionItemDto = new TransactionItemDto();
            transactionItemDto.setTransaction(transactionDto.getId());
            transactionItemDto.setProduct(membershipCreateDto.getProductId());
            transactionItemDto.setProduct(productDto.getId());
            transactionItemDto.setUnitPrice(productDto.getSellingPrice());
            transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure());
            transactionItemDto.setQuantity(new BigDecimal("1"));
            transactionService.addItem(transactionItemDto);
            System.out.println("Product Added");
        }

        if (membershipCreateDto.getDateJoined() != null) {
            TransactionDateDto dateJoined = new TransactionDateDto();
            dateJoined.setTransaction(transactionDto.getId());
            dateJoined.setType(DateType.JOINED);
            dateJoined.setValue(membershipCreateDto.getDateJoined());
            transactionService.addDate(dateJoined);
            System.out.println("Joined Date Added");
        } else {
            TransactionDateDto dateJoined = new TransactionDateDto();
            dateJoined.setTransaction(transactionDto.getId());
            dateJoined.setType(DateType.JOINED);
            dateJoined.setValue(new Date());
            transactionService.addDate(dateJoined);
            System.out.println("Joined Date Added");
        }

        if (membershipCreateDto.getMemberId() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.MAINMEMBER);
            transactionPartnerDto.setPartner(membershipCreateDto.getMemberId());
            transactionService.addPartner(transactionPartnerDto);
            System.out.println("Member Added");
        }

//        if (membershipCreateDto.getSalesRepresentativeId() != null) {
//            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
//            transactionPartnerDto.setTransaction(transactionDto.getId());
//            transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
//            transactionPartnerDto.setPartner(membershipCreateDto.getSalesRepresentativeId());
//            transactionService.addPartner(transactionPartnerDto);
//            System.out.println("Rep Added");
//        }
        MembershipDto membershipDto = new MembershipDto();
        membershipDto.setId(transactionDto.getId());
        return membershipDto;

    }

    @Override
    public MembershipDto get(String id) {
        return null;
    }

    @Override
    public List<MembershipQueryResultDto> search(MembershipQueryDto membershipQueryDto) {
        return null;
    }

    @Override
    public void edit(MembershipEditDto membershipEditDto) {

    }

    @Override
    public void addDependent(DependentDto dependentDto) {

    }

    @Override
    public void removeDependent(DependentDto dependentDto) {

    }


}
