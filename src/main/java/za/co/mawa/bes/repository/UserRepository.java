package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.UserEntity;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    List<UserEntity> findAll(Specification<UserEntity> byCriteria, Sort sort);

    @Query("select u from UserEntity u where u.username = :username")
    UserEntity getByName(@Param("username") String username);

    @Query("select u from UserEntity u where u.email = :email")
    UserEntity getByEmail(@Param("email") String email);

    @Query("select u from UserEntity u where lower(u.email) = lower(:email)")
    UserEntity getByEmailIgnoreCase(@Param("email") String email);

    @Query("select u from UserEntity u where u.cellphone = :cellphone")
    UserEntity getByCellphone(@Param("cellphone") String cellphone);

    @Query("select u from UserEntity u where u.partner = :partnerId")
    UserEntity getByPartner(@Param("partnerId") String partnerId);
}
