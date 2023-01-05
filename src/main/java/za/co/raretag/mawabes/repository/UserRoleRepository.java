package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.raretag.mawabes.entity.UserRoleEntity;
import za.co.raretag.mawabes.entity.UserRolePKEntity;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRolePKEntity> {
}
