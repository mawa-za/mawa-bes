#!/usr/bin/env python3
"""
Mawa BES DTO migrator.

Run from mawa-bes repo root:
    python3 tools/mawa_dto_migrator.py --root . --dry-run
    python3 tools/mawa_dto_migrator.py --root . --write

This tool is conservative and creates DTO/mapper files while patching common controller patterns.
Review all generated files before committing.
"""

from __future__ import annotations

import argparse
import dataclasses
import re
from pathlib import Path
from typing import Iterable

JAVA_SRC = Path("src/main/java")

FIELD_RE = re.compile(
    r"^\s*(?:@[^\n]+\n\s*)*private\s+(?!static\b)(?!final\b)([\w<>?,. ]+)\s+(\w+)\s*(?:=.*)?;",
    re.MULTILINE,
)
PACKAGE_RE = re.compile(r"^package\s+([\w.]+);", re.MULTILINE)
CLASS_RE = re.compile(r"\b(?:public\s+)?class\s+(\w+)")
IMPORT_RE = re.compile(r"^import\s+([\w.]+);", re.MULTILINE)

SKIP_FIELD_NAMES = {
    "serialVersionUID",
}

AUDIT_FIELDS = {
    "createdAt",
    "updatedAt",
    "createdBy",
    "updatedBy",
    "deletedAt",
    "deletedBy",
}

RELATION_ANNOTATIONS = (
    "@ManyToOne",
    "@OneToOne",
    "@OneToMany",
    "@ManyToMany",
)

@dataclasses.dataclass
class JavaField:
    type_name: str
    name: str
    relation: bool = False

@dataclasses.dataclass
class JavaClass:
    path: Path
    package: str
    name: str
    fields: list[JavaField]
    imports: list[str]

    @property
    def domain_name(self) -> str:
        return self.name[:-6] if self.name.endswith("Entity") else self.name


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def write_text(path: Path, text: str, write: bool) -> None:
    if write:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")


def parse_java_class(path: Path) -> JavaClass | None:
    text = read_text(path)
    pkg = PACKAGE_RE.search(text)
    cls = CLASS_RE.search(text)
    if not pkg or not cls:
        return None
    fields: list[JavaField] = []
    for m in FIELD_RE.finditer(text):
        before = text[max(0, m.start() - 400):m.start()]
        relation = any(a in before for a in RELATION_ANNOTATIONS)
        type_name = m.group(1).strip()
        name = m.group(2).strip()
        if name in SKIP_FIELD_NAMES:
            continue
        fields.append(JavaField(type_name=type_name, name=name, relation=relation))
    imports = IMPORT_RE.findall(text)
    return JavaClass(path=path, package=pkg.group(1), name=cls.group(1), fields=fields, imports=imports)


def find_entities(root: Path) -> list[JavaClass]:
    base = root / JAVA_SRC
    entities = []
    for path in base.rglob("*Entity.java"):
        parsed = parse_java_class(path)
        if parsed:
            entities.append(parsed)
    return sorted(entities, key=lambda c: c.name)


def dto_package(entity: JavaClass) -> str:
    return entity.package.replace(".entity", ".dto") if ".entity" in entity.package else entity.package + ".dto"


def mapper_package(entity: JavaClass) -> str:
    return entity.package.replace(".entity", ".mapper") if ".entity" in entity.package else entity.package + ".mapper"


def package_to_dir(root: Path, package: str) -> Path:
    return root / JAVA_SRC / Path(package.replace(".", "/"))


def dto_field_type(field: JavaField) -> str:
    raw = field.type_name.strip()
    # Avoid exposing nested JPA entities. Use id fields in DTOs instead.
    if field.relation:
        if raw.startswith("List<") or raw.startswith("Set<") or raw.startswith("Collection<"):
            return "List<String>"
        return "String"
    # Collections of entities become ids.
    if "Entity" in raw:
        return re.sub(r"\w+Entity", "String", raw)
    return raw


def dto_field_name(field: JavaField) -> str:
    if field.relation:
        return field.name + "Ids" if field.type_name.startswith(("List<", "Set<", "Collection<")) else field.name + "Id"
    return field.name


def java_type_imports(fields: Iterable[JavaField], base_imports: list[str]) -> set[str]:
    imports: set[str] = set()
    text = "\n".join(dto_field_type(f) for f in fields)
    for imp in base_imports:
        simple = imp.rsplit(".", 1)[-1]
        if re.search(rf"\b{re.escape(simple)}\b", text) and ".entity." not in imp:
            imports.add(imp)
    if "List<" in text:
        imports.add("java.util.List")
    if "Set<" in text:
        imports.add("java.util.Set")
    if "BigDecimal" in text:
        imports.add("java.math.BigDecimal")
    if "LocalDateTime" in text:
        imports.add("java.time.LocalDateTime")
    if "LocalDate" in text:
        imports.add("java.time.LocalDate")
    return imports


def lombok_dto(package: str, class_name: str, fields: list[JavaField], imports: set[str]) -> str:
    import_block = "\n".join(f"import {i};" for i in sorted(imports))
    if import_block:
        import_block += "\n\n"
    field_block = "\n".join(f"    private {dto_field_type(f)} {dto_field_name(f)};" for f in fields)
    return f"""package {package};

{import_block}import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {class_name} {{

{field_block}
}}
"""


def generate_dtos(root: Path, entity: JavaClass, write: bool) -> list[Path]:
    pkg = dto_package(entity)
    domain = entity.domain_name
    response_fields = entity.fields
    create_fields = [f for f in entity.fields if f.name not in AUDIT_FIELDS and f.name != "id"]
    update_fields = [f for f in entity.fields if f.name not in AUDIT_FIELDS]
    generated = []
    for suffix, fields in [
        ("ResponseDto", response_fields),
        ("CreateRequestDto", create_fields),
        ("UpdateRequestDto", update_fields),
    ]:
        class_name = f"{domain}{suffix}"
        imports = java_type_imports(fields, entity.imports)
        text = lombok_dto(pkg, class_name, fields, imports)
        path = package_to_dir(root, pkg) / f"{class_name}.java"
        write_text(path, text, write)
        generated.append(path)
    return generated


def cap(name: str) -> str:
    return name[:1].upper() + name[1:]


def generate_mapper(root: Path, entity: JavaClass, write: bool) -> Path:
    domain = entity.domain_name
    pkg = mapper_package(entity)
    dto_pkg = dto_package(entity)
    class_name = f"{domain}Mapper"

    response_sets = []
    create_sets = []
    update_sets = []
    for f in entity.fields:
        if f.relation:
            # Relations must be resolved explicitly in services/repositories.
            continue
        response_sets.append(f"                .{f.name}(entity.get{cap(f.name)}())")
        if f.name != "id" and f.name not in AUDIT_FIELDS:
            create_sets.append(f"                .{f.name}(request.get{cap(f.name)}())")
        if f.name not in AUDIT_FIELDS:
            update_sets.append(f"        entity.set{cap(f.name)}(request.get{cap(f.name)}());")

    # Relation IDs in response are intentionally left as TODO comments because ID getter strategy differs by project.
    relation_comments = []
    for f in entity.fields:
        if f.relation:
            relation_comments.append(f"        // TODO: map relation field `{f.name}` to `{dto_field_name(f)}` once the related entity id getter is confirmed.")

    text = f"""package {pkg};

import org.springframework.stereotype.Component;
import {entity.package}.{entity.name};
import {dto_pkg}.{domain}CreateRequestDto;
import {dto_pkg}.{domain}ResponseDto;
import {dto_pkg}.{domain}UpdateRequestDto;

@Component
public class {class_name} {{

    public {domain}ResponseDto toResponse({entity.name} entity) {{
        if (entity == null) {{
            return null;
        }}
{chr(10).join(relation_comments)}
        return {domain}ResponseDto.builder()
{chr(10).join(response_sets)}
                .build();
    }}

    public {entity.name} toEntity({domain}CreateRequestDto request) {{
        if (request == null) {{
            return null;
        }}
        return {entity.name}.builder()
{chr(10).join(create_sets)}
                .build();
    }}

    public void updateEntity({entity.name} entity, {domain}UpdateRequestDto request) {{
        if (entity == null || request == null) {{
            return;
        }}
{chr(10).join(update_sets)}
    }}
}}
"""
    path = package_to_dir(root, pkg) / f"{class_name}.java"
    write_text(path, text, write)
    return path


def is_controller(path: Path) -> bool:
    if not path.name.endswith(".java"):
        return False
    text = read_text(path)
    return "@RestController" in text or "@Controller" in text


def patch_controller_text(text: str, entities: list[JavaClass]) -> tuple[str, list[str]]:
    changes = []
    patched = text
    imports_to_add: set[str] = set()
    mapper_fields: list[tuple[str, str]] = []

    for e in entities:
        entity_name = e.name
        domain = e.domain_name
        dto_pkg = dto_package(e)
        mapper_pkg = mapper_package(e)
        response = f"{domain}ResponseDto"
        create = f"{domain}CreateRequestDto"
        update = f"{domain}UpdateRequestDto"
        mapper = f"{domain}Mapper"
        mapper_var = domain[:1].lower() + domain[1:] + "Mapper"

        if entity_name not in patched:
            continue

        before = patched
        # Request body entity -> create/update request DTO. Use create by default; PUT/PATCH may require manual update.
        patched = re.sub(rf"(@RequestBody\s+){entity_name}(\s+\w+)", rf"\1{create}\2", patched)
        # Common return wrappers.
        patched = re.sub(rf"ResponseEntity\s*<\s*{entity_name}\s*>", f"ResponseEntity<{response}>", patched)
        patched = re.sub(rf"ResponseEntity\s*<\s*List\s*<\s*{entity_name}\s*>\s*>", f"ResponseEntity<List<{response}>>", patched)
        patched = re.sub(rf"List\s*<\s*{entity_name}\s*>", f"List<{response}>", patched)
        patched = re.sub(rf"\b{entity_name}\b(?=\s+\w+\s*[=;,)])", response, patched)

        if patched != before:
            changes.append(f"Patched references for {entity_name} -> DTOs")
            imports_to_add.update({
                f"{dto_pkg}.{response}",
                f"{dto_pkg}.{create}",
                f"{dto_pkg}.{update}",
                f"{mapper_pkg}.{mapper}",
            })
            mapper_fields.append((mapper, mapper_var))

    if changes:
        # Add imports before class.
        existing_imports = set(IMPORT_RE.findall(patched))
        missing = [i for i in sorted(imports_to_add) if i not in existing_imports]
        if missing:
            import_block = "\n".join(f"import {i};" for i in missing) + "\n"
            last_import = list(IMPORT_RE.finditer(patched))[-1] if list(IMPORT_RE.finditer(patched)) else None
            if last_import:
                insert_at = last_import.end()
                patched = patched[:insert_at] + "\n" + import_block + patched[insert_at:]

        # Add mapper fields if Lombok RequiredArgsConstructor is used; otherwise leave a TODO comment.
        class_match = re.search(r"(public\s+class\s+\w+\s*\{)", patched)
        if class_match:
            field_lines = []
            for mapper, mapper_var in mapper_fields:
                if mapper_var not in patched:
                    field_lines.append(f"\n    private final {mapper} {mapper_var};")
            if field_lines:
                patched = patched[:class_match.end()] + "\n" + "".join(field_lines) + patched[class_match.end():]
                changes.append("Added mapper fields to controller; ensure @RequiredArgsConstructor or constructor injection is present")

        changes.append("Manual review required: wrap service entity returns with mapper.toResponse(...) and list streams")

    return patched, changes


def patch_controllers(root: Path, entities: list[JavaClass], write: bool) -> dict[Path, list[str]]:
    changed = {}
    for path in (root / JAVA_SRC).rglob("*Controller.java"):
        if not is_controller(path):
            continue
        text = read_text(path)
        patched, changes = patch_controller_text(text, entities)
        if changes and patched != text:
            write_text(path, patched, write)
            changed[path] = changes
    return changed


def write_report(root: Path, entities: list[JavaClass], dto_files: list[Path], mapper_files: list[Path], controller_changes: dict[Path, list[str]], write: bool) -> Path:
    lines = [
        "# Mawa DTO Migration Report",
        "",
        f"Entities detected: {len(entities)}",
        f"DTO files generated: {len(dto_files)}",
        f"Mapper files generated: {len(mapper_files)}",
        f"Controllers patched: {len(controller_changes)}",
        "",
        "## Entities",
        "",
    ]
    for e in entities:
        lines.append(f"- `{e.name}` -> `{dto_package(e)}.{e.domain_name}ResponseDto`, `{e.domain_name}CreateRequestDto`, `{e.domain_name}UpdateRequestDto`")
    lines += ["", "## Controller changes", ""]
    for p, changes in controller_changes.items():
        lines.append(f"### `{p.relative_to(root)}`")
        for c in changes:
            lines.append(f"- {c}")
        lines.append("")
    lines += [
        "## Required follow-up",
        "",
        "- Replace direct controller returns of service entities with mapper calls.",
        "- Resolve relation ids in generated mappers where TODO comments were inserted.",
        "- Run `mvn test` and fix compile errors caused by project-specific service signatures.",
        "- Remove JSON recursion annotations that were only needed because entities were exposed.",
    ]
    path = root / "build/mawa-dto-migration-report.md"
    write_text(path, "\n".join(lines) + "\n", write)
    return path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".", help="Repository root")
    parser.add_argument("--write", action="store_true", help="Write generated files and patches")
    parser.add_argument("--dry-run", action="store_true", help="Show actions without writing")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    if not (root / JAVA_SRC).exists():
        raise SystemExit(f"Could not find {JAVA_SRC} under {root}")

    write = args.write and not args.dry_run
    entities = find_entities(root)
    dto_files: list[Path] = []
    mapper_files: list[Path] = []
    for entity in entities:
        dto_files.extend(generate_dtos(root, entity, write))
        mapper_files.append(generate_mapper(root, entity, write))

    controller_changes = patch_controllers(root, entities, write)
    report = write_report(root, entities, dto_files, mapper_files, controller_changes, write)

    print(f"Entities detected: {len(entities)}")
    print(f"DTO files {'generated' if write else 'planned'}: {len(dto_files)}")
    print(f"Mapper files {'generated' if write else 'planned'}: {len(mapper_files)}")
    print(f"Controllers {'patched' if write else 'planned'}: {len(controller_changes)}")
    print(f"Report: {report}")
    if not write:
        print("Dry run only. Re-run with --write to apply changes.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
