package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.AddressEntity;
import za.co.mawa.bes.entity.BankAccountEntity;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity,String> {
    @Query("SELECT a FROM AddressEntity a WHERE a.objectId = :objectId")
    List<AddressEntity> getByObjectId(String objectId);
}
