package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.exception.*;

import java.util.List;

public interface MembershipDao {
    String create(MembershipCreateDto membershipCreateDto) throws PartnerNotFoundException, ProductNotFoundException, TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException;
    MembershipDto get(String id);
    List<MembershipDto> search(MembershipQueryDto membershipQueryDto);
    void edit(String id, MembershipEditDto membershipEditDto);
    void addDependent(DependentDto dependentDto);
    void removeDependent(DependentDto dependentDto);
}
