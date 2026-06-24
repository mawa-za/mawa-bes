# Mawa BES DTO Migration Kit

This kit migrates Spring Boot endpoints away from exposing JPA entity classes directly in request payloads and API responses.

Because the repository could not be cloned from this environment, run this kit inside your local checkout of:

```bash
git clone --branch master https://github.com/mawa-za/mawa-bes.git
cd mawa-bes
```

Then copy the `tools` folder into the repo root and run:

```bash
python3 tools/mawa_dto_migrator.py --root . --dry-run
python3 tools/mawa_dto_migrator.py --root . --write
```

The script will:

1. Find classes ending with `Entity` under `src/main/java`.
2. Generate DTO classes under matching `dto` packages.
3. Generate mapper classes under matching `mapper` packages.
4. Patch controllers so request bodies use `Create/UpdateRequestDto` and responses use `ResponseDto`.
5. Produce a migration report at `build/mawa-dto-migration-report.md`.

## Recommended manual review after generation

The migrator is intentionally conservative. After running it, review:

- Controllers with complex custom response shapes.
- Services that currently return entities.
- Endpoints that accept entity IDs through nested entity objects.
- Any `@JsonIgnore`, `@JsonManagedReference`, or `@JsonBackReference` workarounds that can now be removed.
- OpenAPI/Swagger schemas.

## Target architecture

Controllers should only expose DTOs:

```java
@PostMapping
public ResponseEntity<MembershipResponseDto> create(@RequestBody MembershipCreateRequestDto request) {
    MembershipEntity entity = membershipService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(membershipMapper.toResponse(entity));
}
```

Services may accept DTOs for use-case methods, but repositories must continue using entities:

```java
public MembershipEntity create(MembershipCreateRequestDto request) {
    MembershipEntity entity = membershipMapper.toEntity(request);
    return membershipRepository.save(entity);
}
```

Entities should not be used as endpoint contracts.
