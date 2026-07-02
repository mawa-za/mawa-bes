# Controller Refactor Checklist

For every controller:

- [ ] No method parameter uses an `Entity` class.
- [ ] No return type exposes an `Entity` class.
- [ ] `@RequestBody` uses a Create/Update request DTO.
- [ ] `ResponseEntity<T>` uses response DTOs.
- [ ] Lists are mapped with `.stream().map(mapper::toResponse).toList()`.
- [ ] Relations are returned as IDs or summary DTOs.
- [ ] Entity validation annotations copied to request DTOs where appropriate.
- [ ] Services resolve related entities by IDs from DTOs.
- [ ] No controller contains mapping logic beyond calling a mapper.
- [ ] Swagger/OpenAPI examples updated.

Common mapping examples:

```java
return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
```

```java
Entity saved = service.create(request);
return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
```
