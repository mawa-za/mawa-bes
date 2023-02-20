package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.FieldEntity;


import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<FieldEntity, String> {

}
