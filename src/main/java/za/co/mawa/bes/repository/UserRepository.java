package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.TransactionAmountEntity;
import za.co.mawa.bes.entity.UserEntity;

import java.util.List;

@Repository
public interface UserRepository  extends JpaRepository<UserEntity,String>{
    @Query(value = "SELECT * FROM user u WHERE u.username = :username LIMIT 1", nativeQuery = true)
    UserEntity getByName(String username);
}
