package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.UserEntity;

import java.util.List;

@Repository
public interface UserRepository  extends JpaRepository<UserEntity,String>{
    List<UserEntity> findAll(Specification<UserEntity> byCriteria, Sort sort);
    @Query(value = "SELECT * FROM user u WHERE u.username = :username LIMIT 1", nativeQuery = true)
    UserEntity getByName(String username);
    @Query(value = "SELECT * FROM user u WHERE u.email = :email LIMIT 1", nativeQuery = true)
    UserEntity getByEmail(String email);
    @Query(value = "SELECT * FROM user u WHERE u.cellphone = :cellphone LIMIT 1", nativeQuery = true)
    UserEntity getByCellphone(String cellphone); 
    @Query(value = "SELECT * FROM user u WHERE u.partner = :partnerId LIMIT 1", nativeQuery = true)
    UserEntity getByPartner(String partnerId);
}
