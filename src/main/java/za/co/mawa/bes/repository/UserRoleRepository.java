package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.entity.UserRolePKEntity;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRolePKEntity> {
    @Query("SELECT r FROM UserRoleEntity r WHERE r.user = :user")
    List<UserRoleEntity> findUserRoles(@Param("user") String user);

}
