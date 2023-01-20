package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.FieldOptionEntity;
import za.co.mawa.bes.entity.FieldOptionPKEntity;
import za.co.mawa.bes.entity.UserRoleEntity;

import java.util.List;

@Repository
public interface FieldOptionRepository extends JpaRepository<FieldOptionEntity, FieldOptionPKEntity> {
    @Query("SELECT f FROM field_option f WHERE f.field = :field")
    List<FieldOptionEntity> findFieldOptions(@Param("field") String field);
}
