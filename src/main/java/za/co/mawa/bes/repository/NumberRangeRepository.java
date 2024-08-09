package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.NumberRangeEntity;

@Repository
public interface NumberRangeRepository extends JpaRepository<NumberRangeEntity,String> {
    @Query(value = "SELECT * FROM number_range n WHERE n.object = :object limit 1", nativeQuery = true)
    NumberRangeEntity getRangeByObject(String object);

    @Procedure("GET_NEW_NUMBER")
    String getNewNumber(String object);
}
