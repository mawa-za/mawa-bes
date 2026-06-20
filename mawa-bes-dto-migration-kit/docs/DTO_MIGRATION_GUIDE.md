# DTO Migration Guide for mawa-bes

## Problem

The current endpoint contracts are coupled to JPA entity classes. This causes several issues:

- API clients see persistence fields and relationships that should be internal.
- Bidirectional JPA relationships can trigger infinite JSON recursion.
- Lazy-loaded relations can fail during serialization.
- Entity changes accidentally become API breaking changes.
- Validation rules are difficult to separate between create, update, and response models.

## Target pattern

Use three DTOs per aggregate/entity where practical:

- `XCreateRequestDto` for POST payloads.
- `XUpdateRequestDto` for PUT/PATCH payloads.
- `XResponseDto` for response payloads.

For relations, expose IDs or small nested summary DTOs, not full entities.

Example:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResponseDto {
    private String id;
    private String memberId;
    private String planId;
    private String status;
    private LocalDate paidUpToPeriod;
    private LocalDateTime createdAt;
}
```

## Controller rule

Controllers must not use `Entity` in public method signatures.

Bad:

```java
@PostMapping
public MembershipEntity create(@RequestBody MembershipEntity request) { ... }
```

Good:

```java
@PostMapping
public ResponseEntity<MembershipResponseDto> create(@RequestBody MembershipCreateRequestDto request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(membershipMapper.toResponse(service.create(request)));
}
```

## Service rule

Services may accept request DTOs if the method represents an API use case. Repositories must remain entity-based.

```java
@Transactional
public MembershipEntity create(MembershipCreateRequestDto request) {
    MembershipEntity entity = membershipMapper.toEntity(request);
    entity.setPlan(planRepository.getReferenceById(request.getPlanId()));
    entity.setMember(partnerRepository.getReferenceById(request.getMemberId()));
    return membershipRepository.save(entity);
}
```

## Mapper rule

Keep mapping code out of controllers. Use one mapper per aggregate.

```java
@Component
public class MembershipMapper {
    public MembershipResponseDto toResponse(MembershipEntity entity) { ... }
    public MembershipEntity toEntity(MembershipCreateRequestDto dto) { ... }
    public void updateEntity(MembershipEntity entity, MembershipUpdateRequestDto dto) { ... }
}
```

## Validation rule

Validation annotations belong on request DTOs, not entities, unless they are true persistence invariants.

```java
@NotBlank
private String deceasedName;
```

## API compatibility

Do not change endpoint paths while doing this migration. Keep paths like `/v2/cases`, `/v2/funeral/...`, `/v2/membership/...`.
