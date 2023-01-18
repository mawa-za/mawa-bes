package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.raretag.mawabes.entity.DocumentLinkEntity;
import za.co.raretag.mawabes.entity.DocumentLinkPKEntity;
import za.co.raretag.mawabes.service.DocumentLinkService;

public interface DocumentLinkRepository extends JpaRepository<DocumentLinkEntity, DocumentLinkPKEntity> {
}
