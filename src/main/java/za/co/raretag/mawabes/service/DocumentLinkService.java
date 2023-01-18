package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import za.co.raretag.mawabes.dao.DocumentLinkDao;
import za.co.raretag.mawabes.dto.DocumentLinkDto;
import za.co.raretag.mawabes.entity.DocumentLinkPKEntity;
import za.co.raretag.mawabes.repository.DocumentLinkRepository;

public class DocumentLinkService implements DocumentLinkDao {
    @Autowired
    DocumentLinkRepository documentLinkRepository;
    @Override
    public void remove(DocumentLinkDto link) {
        if (link != null) {
            try {
                DocumentLinkPKEntity documentLinkPkEntity = new DocumentLinkPKEntity();

                documentLinkPkEntity.setChild(link.getChild());
                documentLinkPkEntity.setParent(link.getParent());
                documentLinkPkEntity.setType(link.getType());
                //documentLinkRepository.delete(documentLinkPkEntity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
