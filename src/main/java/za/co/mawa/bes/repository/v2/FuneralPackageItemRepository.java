package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.FuneralPackageItemEntity;
import java.util.List;
public interface FuneralPackageItemRepository extends JpaRepository<FuneralPackageItemEntity,String> {
    List<FuneralPackageItemEntity> findByFuneralPackageIdOrderByProductDescriptionAsc(String funeralPackageId);
    void deleteByFuneralPackageId(String funeralPackageId);
}
