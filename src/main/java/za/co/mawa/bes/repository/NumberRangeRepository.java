package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.NumberRangeEntity;

@Repository
public interface NumberRangeRepository extends JpaRepository<NumberRangeEntity,String> {
}
