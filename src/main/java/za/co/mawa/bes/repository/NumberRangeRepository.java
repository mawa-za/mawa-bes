package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.NumberRangeEntity;

import java.util.List;

@Repository
public interface NumberRangeRepository extends JpaRepository<NumberRangeEntity, Integer> {
    @Query(value = "SELECT * FROM number_range n WHERE n.object = :object LIMIT 1", nativeQuery = true)
    NumberRangeEntity getRangeByObject(String object);

    boolean existsByObject(String object);

    List<NumberRangeEntity> findAllByOrderByObjectAsc();

    @Procedure("GetNewNumber")
    String getNewNumber(String object);
}
