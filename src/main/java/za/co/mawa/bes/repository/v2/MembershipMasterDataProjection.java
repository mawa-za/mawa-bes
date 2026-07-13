package za.co.mawa.bes.repository.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public interface MembershipMasterDataProjection {
    String getMembershipId();
    String getMembershipNo();
    String getPartnerId();
    String getPlanId();
    Long getPremiumCents();
    LocalDate getStartDate();
    LocalDate getJoinDate();
    String getMembershipStatus();
    String getPaidUpToPeriod();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    String getPartnerNo();
    String getPartnerType();
    String getName1();
    String getName2();
    String getName3();
    String getIdentityType();
    String getIdentityNumber();
    Date getBirthDate();
    String getGender();
    String getPartnerStatus();
}
