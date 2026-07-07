package za.co.mawa.bes.repository.v2.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;

@Repository
public interface CompanyLogoRepository extends JpaRepository<CompanyLogoEntity, String> {
}
