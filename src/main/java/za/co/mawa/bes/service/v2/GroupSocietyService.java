package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.group.GroupSocietyAdjustmentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyClaimDebitRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyContactRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyMemberRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyPaymentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyRequest;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyContactEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyMemberEntity;
import za.co.mawa.bes.repository.v2.GroupSocietyAccountTxnRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyContactRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyMemberRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class GroupSocietyService {

    private final GroupSocietyRepository groupSocietyRepository;
    private final GroupSocietyContactRepository contactRepository;
    private final GroupSocietyMemberRepository memberRepository;
    private final GroupSocietyAccountTxnRepository accountTxnRepository;

    /*
     * Inject your existing PartnerRepository here if available.
     *
     * Example:
     *
     * private final PartnerRepository partnerRepository;
     *
     * Then validate:
     * PartnerEntity partner = partnerRepository.findById(request.getPartnerId())
     *      .orElseThrow(() -> new RuntimeException("Partner not found"));
     *
     * if (!"GROUP".equalsIgnoreCase(partner.getType())) {
     *      throw new RuntimeException("Partner must be of type GROUP");
     * }
     */

    public GroupSocietyService(
            GroupSocietyRepository groupSocietyRepository,
            GroupSocietyContactRepository contactRepository,
            GroupSocietyMemberRepository memberRepository,
            GroupSocietyAccountTxnRepository accountTxnRepository
    ) {
        this.groupSocietyRepository = groupSocietyRepository;
        this.contactRepository = contactRepository;
        this.memberRepository = memberRepository;
        this.accountTxnRepository = accountTxnRepository;
    }

    public List<GroupSocietyEntity> getAll(String status, String societyType) {
        if (status != null && !status.isBlank()) {
            return groupSocietyRepository.findByStatus(status);
        }

        if (societyType != null && !societyType.isBlank()) {
            return groupSocietyRepository.findBySocietyType(societyType);
        }

        return groupSocietyRepository.findAll();
    }

    public GroupSocietyEntity getById(String id) {
        return groupSocietyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group society not found: " + id));
    }

    public GroupSocietyEntity getByGroupNo(String groupNo) {
        return groupSocietyRepository.findByGroupNo(groupNo)
                .orElseThrow(() -> new RuntimeException("Group society not found: " + groupNo));
    }

    public GroupSocietyEntity getByPartnerId(String partnerId) {
        return groupSocietyRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new RuntimeException("Group society not found for partner: " + partnerId));
    }

    @Transactional
    public GroupSocietyEntity create(GroupSocietyRequest request) {
        validateGroupSocietyRequest(request);

        if (groupSocietyRepository.existsByPartnerId(request.getPartnerId())) {
            throw new RuntimeException("This partner is already linked to a group society");
        }

        if (groupSocietyRepository.existsByGroupNo(request.getGroupNo())) {
            throw new RuntimeException("Group society number already exists: " + request.getGroupNo());
        }

        /*
         * Validate partner exists and partner.type = GROUP here using your existing PartnerRepository.
         * The partner is the customer master record.
         * group_society only stores prepaid account information.
         */

        Long openingBalance = safeLong(request.getOpeningBalanceCents());

        if (openingBalance < 0) {
            throw new RuntimeException("Opening balance cannot be negative");
        }

        GroupSocietyEntity entity = new GroupSocietyEntity();

        entity.setPartnerId(request.getPartnerId());
        entity.setGroupNo(request.getGroupNo());
        entity.setSocietyType(request.getSocietyType());
        entity.setStatus(defaultValue(request.getStatus(), "ACTIVE"));

        entity.setAvailableBalanceCents(openingBalance);
        entity.setTotalPaidCents(0L);
        entity.setTotalClaimedCents(0L);

        GroupSocietyEntity saved = groupSocietyRepository.save(entity);

        if (openingBalance > 0) {
            GroupSocietyAccountTxnEntity txn = new GroupSocietyAccountTxnEntity();
            txn.setGroupSocietyId(saved.getId());
            txn.setTxnType("ADJUSTMENT_CREDIT");
            txn.setDirection("CREDIT");
            txn.setAmountCents(openingBalance);
            txn.setBalanceBeforeCents(0L);
            txn.setBalanceAfterCents(openingBalance);
            txn.setTxnDate(LocalDate.now());
            txn.setReferenceType("OPENING_BALANCE");
            txn.setReferenceNo("OPENING-BALANCE");
            txn.setNotes("Opening balance loaded on group society creation");

            accountTxnRepository.save(txn);
        }

        return saved;
    }

    @Transactional
    public GroupSocietyEntity update(String id, GroupSocietyRequest request) {
        GroupSocietyEntity entity = getById(id);

        if (request.getSocietyType() != null) {
            entity.setSocietyType(request.getSocietyType());
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        return groupSocietyRepository.save(entity);
    }

    @Transactional
    public GroupSocietyEntity activate(String id) {
        GroupSocietyEntity entity = getById(id);
        entity.setStatus("ACTIVE");
        return groupSocietyRepository.save(entity);
    }

    @Transactional
    public GroupSocietyEntity suspend(String id) {
        GroupSocietyEntity entity = getById(id);
        entity.setStatus("SUSPENDED");
        return groupSocietyRepository.save(entity);
    }

    @Transactional
    public GroupSocietyEntity close(String id) {
        GroupSocietyEntity entity = getById(id);
        entity.setStatus("CLOSED");
        return groupSocietyRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        getById(id);

        accountTxnRepository.findByGroupSocietyIdOrderByTxnDatetimeDesc(id)
                .forEach(accountTxnRepository::delete);

        memberRepository.deleteByGroupSocietyId(id);
        contactRepository.deleteByGroupSocietyId(id);
        groupSocietyRepository.deleteById(id);
    }

    public List<GroupSocietyContactEntity> getContacts(String groupSocietyId) {
        getById(groupSocietyId);
        return contactRepository.findByGroupSocietyId(groupSocietyId);
    }

    @Transactional
    public GroupSocietyContactEntity addContact(String groupSocietyId, GroupSocietyContactRequest request) {
        getById(groupSocietyId);

        if (request.getContactName() == null || request.getContactName().isBlank()) {
            throw new RuntimeException("Contact name is required");
        }

        GroupSocietyContactEntity entity = new GroupSocietyContactEntity();
        entity.setGroupSocietyId(groupSocietyId);
        entity.setContactName(request.getContactName());
        entity.setRole(request.getRole());
        entity.setMobileNo(request.getMobileNo());
        entity.setEmail(request.getEmail());
        entity.setPrimaryContact(Boolean.TRUE.equals(request.getPrimaryContact()));

        return contactRepository.save(entity);
    }

    @Transactional
    public void deleteContact(String contactId) {
        contactRepository.deleteById(contactId);
    }

    public List<GroupSocietyMemberEntity> getMembers(String groupSocietyId, String status) {
        getById(groupSocietyId);

        if (status != null && !status.isBlank()) {
            return memberRepository.findByGroupSocietyIdAndStatus(groupSocietyId, status);
        }

        return memberRepository.findByGroupSocietyId(groupSocietyId);
    }

    @Transactional
    public GroupSocietyMemberEntity addMember(String groupSocietyId, GroupSocietyMemberRequest request) {
        getById(groupSocietyId);

        if (request.getMemberId() == null || request.getMemberId().isBlank()) {
            throw new RuntimeException("memberId is required");
        }

        if (memberRepository.existsByGroupSocietyIdAndMemberId(groupSocietyId, request.getMemberId())) {
            throw new RuntimeException("Member already exists in this group society");
        }

        GroupSocietyMemberEntity entity = new GroupSocietyMemberEntity();

        entity.setGroupSocietyId(groupSocietyId);
        entity.setMemberId(request.getMemberId());
        entity.setMembershipId(request.getMembershipId());
        entity.setEmployeeNo(request.getEmployeeNo());
        entity.setExternalRef(request.getExternalRef());
        entity.setJoinDate(request.getJoinDate() != null ? request.getJoinDate() : LocalDate.now());
        entity.setStatus(defaultValue(request.getStatus(), "ACTIVE"));

        return memberRepository.save(entity);
    }

    @Transactional
    public GroupSocietyMemberEntity removeMember(String groupSocietyId, String memberId) {
        GroupSocietyMemberEntity entity = memberRepository
                .findByGroupSocietyIdAndMemberId(groupSocietyId, memberId)
                .orElseThrow(() -> new RuntimeException("Member not found in group society"));

        entity.setStatus("EXITED");
        entity.setExitDate(LocalDate.now());

        return memberRepository.save(entity);
    }

    @Transactional
    public GroupSocietyAccountTxnEntity recordPayment(String groupSocietyId, GroupSocietyPaymentRequest request) {
        validateAmount(request.getAmountCents());

        GroupSocietyEntity society = getByIdForUpdate(groupSocietyId);
        validateGroupIsOpenForPosting(society);

        if (request.getReferenceId() != null &&
                accountTxnRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                        "RECEIPT",
                        request.getReferenceId(),
                        "PAYMENT"
                )) {
            throw new RuntimeException("Payment already posted for this receipt/reference");
        }

        Long balanceBefore = safeLong(society.getAvailableBalanceCents());
        Long amount = request.getAmountCents();
        Long balanceAfter = balanceBefore + amount;

        LocalDate paymentDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();

        society.setAvailableBalanceCents(balanceAfter);
        society.setTotalPaidCents(safeLong(society.getTotalPaidCents()) + amount);
        society.setLastPaymentDate(paymentDate);

        groupSocietyRepository.save(society);

        GroupSocietyAccountTxnEntity txn = new GroupSocietyAccountTxnEntity();
        txn.setGroupSocietyId(groupSocietyId);
        txn.setTxnType("PAYMENT");
        txn.setDirection("CREDIT");
        txn.setAmountCents(amount);
        txn.setBalanceBeforeCents(balanceBefore);
        txn.setBalanceAfterCents(balanceAfter);
        txn.setTxnDate(paymentDate);
        txn.setReferenceType("RECEIPT");
        txn.setReferenceId(request.getReferenceId());
        txn.setReferenceNo(request.getReferenceNo());
        txn.setPaymentMethod(request.getPaymentMethod());
        txn.setPeriod(request.getPeriod());
        txn.setNotes(request.getNotes());

        return accountTxnRepository.save(txn);
    }

    @Transactional
    public GroupSocietyAccountTxnEntity debitClaim(String groupSocietyId, GroupSocietyClaimDebitRequest request) {
        validateAmount(request.getAmountCents());

        GroupSocietyEntity society = getByIdForUpdate(groupSocietyId);
        validateGroupIsOpenForPosting(society);

        if (request.getClaimId() != null &&
                accountTxnRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                        "CLAIM",
                        request.getClaimId(),
                        "CLAIM"
                )) {
            throw new RuntimeException("Claim already debited from this group society");
        }

        Long balanceBefore = safeLong(society.getAvailableBalanceCents());
        Long amount = request.getAmountCents();

        if (balanceBefore < amount) {
            throw new RuntimeException(
                    "Insufficient group society balance. Available: "
                            + balanceBefore
                            + ", Required: "
                            + amount
            );
        }

        Long balanceAfter = balanceBefore - amount;
        LocalDate claimDate = request.getClaimDate() != null ? request.getClaimDate() : LocalDate.now();

        society.setAvailableBalanceCents(balanceAfter);
        society.setTotalClaimedCents(safeLong(society.getTotalClaimedCents()) + amount);
        society.setLastClaimDate(claimDate);

        groupSocietyRepository.save(society);

        GroupSocietyAccountTxnEntity txn = new GroupSocietyAccountTxnEntity();
        txn.setGroupSocietyId(groupSocietyId);
        txn.setTxnType("CLAIM");
        txn.setDirection("DEBIT");
        txn.setAmountCents(amount);
        txn.setBalanceBeforeCents(balanceBefore);
        txn.setBalanceAfterCents(balanceAfter);
        txn.setTxnDate(claimDate);
        txn.setReferenceType("CLAIM");
        txn.setReferenceId(request.getClaimId());
        txn.setReferenceNo(request.getClaimNo());
        txn.setNotes(request.getNotes());

        return accountTxnRepository.save(txn);
    }

    @Transactional
    public GroupSocietyAccountTxnEntity adjustBalance(String groupSocietyId, GroupSocietyAdjustmentRequest request) {
        validateAmount(request.getAmountCents());

        if (request.getDirection() == null || request.getDirection().isBlank()) {
            throw new RuntimeException("direction is required. Use CREDIT or DEBIT");
        }

        String direction = request.getDirection().trim().toUpperCase();

        if (!direction.equals("CREDIT") && !direction.equals("DEBIT")) {
            throw new RuntimeException("Invalid adjustment direction. Use CREDIT or DEBIT");
        }

        GroupSocietyEntity society = getByIdForUpdate(groupSocietyId);
        validateGroupIsOpenForPosting(society);

        Long balanceBefore = safeLong(society.getAvailableBalanceCents());
        Long amount = request.getAmountCents();
        Long balanceAfter;
        String txnType;

        if (direction.equals("CREDIT")) {
            balanceAfter = balanceBefore + amount;
            txnType = "ADJUSTMENT_CREDIT";
        } else {
            if (balanceBefore < amount) {
                throw new RuntimeException("Insufficient balance for debit adjustment");
            }

            balanceAfter = balanceBefore - amount;
            txnType = "ADJUSTMENT_DEBIT";
        }

        LocalDate adjustmentDate = request.getAdjustmentDate() != null
                ? request.getAdjustmentDate()
                : LocalDate.now();

        society.setAvailableBalanceCents(balanceAfter);
        groupSocietyRepository.save(society);

        GroupSocietyAccountTxnEntity txn = new GroupSocietyAccountTxnEntity();
        txn.setGroupSocietyId(groupSocietyId);
        txn.setTxnType(txnType);
        txn.setDirection(direction);
        txn.setAmountCents(amount);
        txn.setBalanceBeforeCents(balanceBefore);
        txn.setBalanceAfterCents(balanceAfter);
        txn.setTxnDate(adjustmentDate);
        txn.setReferenceType("MANUAL_ADJUSTMENT");
        txn.setReferenceNo(request.getReferenceNo());
        txn.setNotes(request.getNotes());

        return accountTxnRepository.save(txn);
    }

    public List<GroupSocietyAccountTxnEntity> getStatement(String groupSocietyId, String period) {
        getById(groupSocietyId);

        if (period != null && !period.isBlank()) {
            return accountTxnRepository.findByGroupSocietyIdAndPeriodOrderByTxnDatetimeDesc(groupSocietyId, period);
        }

        return accountTxnRepository.findByGroupSocietyIdOrderByTxnDatetimeDesc(groupSocietyId);
    }

    private GroupSocietyEntity getByIdForUpdate(String id) {
        return groupSocietyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Group society not found: " + id));
    }

    private void validateGroupSocietyRequest(GroupSocietyRequest request) {
        if (request.getPartnerId() == null || request.getPartnerId().isBlank()) {
            throw new RuntimeException("partnerId is required");
        }

        if (request.getGroupNo() == null || request.getGroupNo().isBlank()) {
            throw new RuntimeException("groupNo is required");
        }
    }

    private void validateGroupIsOpenForPosting(GroupSocietyEntity society) {
        if (!"ACTIVE".equalsIgnoreCase(society.getStatus())) {
            throw new RuntimeException("Cannot post transaction. Group society is not ACTIVE");
        }
    }

    private void validateAmount(Long amountCents) {
        if (amountCents == null || amountCents <= 0) {
            throw new RuntimeException("amountCents must be greater than zero");
        }
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}