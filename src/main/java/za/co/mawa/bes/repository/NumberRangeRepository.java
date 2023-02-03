package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.NumberRangeEntity;
import za.co.mawa.bes.entity.TransactionAmountEntity;

import java.util.List;

@Repository
public interface NumberRangeRepository extends JpaRepository<NumberRangeEntity,String> {
    @Query(value = "SELECT * FROM number_range n WHERE n.object = :object limit 1", nativeQuery = true)
    NumberRangeEntity getRangeByObject(String object);
}
