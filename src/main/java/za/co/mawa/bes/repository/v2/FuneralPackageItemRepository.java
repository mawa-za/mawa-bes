package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.FuneralPackageItemEntity;

import java.util.List;

public interface FuneralPackageItemRepository extends JpaRepository<FuneralPackageItemEntity, String> {
    List<FuneralPackageItemEntity> findByFuneralPackageIdOrderByProductDescriptionAsc(String funeralPackageId);

    @Modifying(flushAutomatically = true)
    @Query("delete from FuneralPackageItemEntity item where item.funeralPackageId = :funeralPackageId")
    int deleteByFuneralPackageId(@Param("funeralPackageId") String funeralPackageId);
}
