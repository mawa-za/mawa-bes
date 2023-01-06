package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.raretag.mawabes.entity.NumberRangeEntity;


public interface NumberRangeRepository extends JpaRepository<NumberRangeEntity,String> {
}
