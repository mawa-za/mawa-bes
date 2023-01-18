package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.raretag.mawabes.entity.TestEntity;
import za.co.raretag.mawabes.entity.UserEntity;

@Repository
public interface TestRepository extends JpaRepository<TestEntity,Long>{

}
