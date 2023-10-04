package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.membership.*;

import java.util.List;

public interface MembershipDao {
    MembershipDto create(MembershipCreateDto membershipCreateDto);
    MembershipDto get(String id);
    List<MembershipQueryResultDto> search(MembershipQueryDto membershipQueryDto);
    void edit(MembershipEditDto membershipEditDto);
    void addDependent(DependentDto dependentDto);
    void removeDependent(DependentDto dependentDto);
}
