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
@Query("SELECT u FROM UserRoleEntity u WHERE u.userRolePKEntity.user = :user")
    List<UserRoleEntity> findUserRoles(@Param("user") String user);
    @Query("SELECT u FROM UserRoleEntity u WHERE u.userRolePKEntity.role = :role")
    List<UserRoleEntity> findRoles(@Param("role") String role);

    @Query(value = "SELECT COUNT(*) FROM user_role ur JOIN `user` u ON u.id = ur.user WHERE ur.role = :role AND u.status = 'ACTIVE' AND (u.expires_at IS NULL OR u.expires_at > NOW())", nativeQuery = true)
    long countActiveUsersByRole(@Param("role") String role);

    @Query(value = "SELECT COUNT(DISTINCT ur.user) FROM user_role ur JOIN `role` r ON r.id = ur.role JOIN `user` u ON u.id = ur.user WHERE r.access_all_workcentres = b'1' AND u.status = 'ACTIVE' AND (u.expires_at IS NULL OR u.expires_at > NOW()) AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE) AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)", nativeQuery = true)
    long countActiveAccessAllUsers();
}
