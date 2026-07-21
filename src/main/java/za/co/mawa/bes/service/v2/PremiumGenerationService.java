package za.co.mawa.bes.service.v2;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;
@Service @RequiredArgsConstructor
public class PremiumGenerationService {
 private final JdbcTemplate jdbc; private final MembershipPremiumService premiums; private final MembershipChangeService membershipChanges;
 public Map<String,Object> configuration(){return jdbc.queryForMap("SELECT * FROM premium_generation_configuration WHERE id='DEFAULT'");}
 @Transactional public Map<String,Object> saveConfiguration(Map<String,Object> r,String user){
  String mode=Objects.toString(r.getOrDefault("generationMode","FIRST_DAY_OF_MONTH")).toUpperCase();
  if(!Set.of("FIRST_DAY_OF_MONTH","MONTH_AFTER_LAST_PAYMENT").contains(mode))throw new IllegalArgumentException("Unsupported generation mode");
  boolean enabled=Boolean.parseBoolean(Objects.toString(r.getOrDefault("enabled","true")));
  jdbc.update("UPDATE premium_generation_configuration SET generation_mode=?,enabled=?,updated_at=CURRENT_TIMESTAMP,updated_by=? WHERE id='DEFAULT'",mode,enabled,user); return configuration();}
 @Transactional public Map<String,Object> backfillSixPeriods(String user){return generate(LocalDate.now().minusMonths(5),LocalDate.now(),user);}
 @Scheduled(cron="${mawa.premium-generation.cron:0 15 1 * * *}") public void scheduled(){
  Map<String,Object> c=configuration(); if(!Boolean.TRUE.equals(c.get("enabled")) && !(c.get("enabled") instanceof Number n && n.intValue()==1))return;
  String mode=Objects.toString(c.get("generation_mode"),"FIRST_DAY_OF_MONTH"); LocalDate now=LocalDate.now();
  if("FIRST_DAY_OF_MONTH".equals(mode)){if(now.getDayOfMonth()!=1)return;generate(now,now,"SYSTEM");}
  else {generateMonthAfterLastPayment("SYSTEM");}
  jdbc.update("UPDATE premium_generation_configuration SET last_run_at=CURRENT_TIMESTAMP WHERE id='DEFAULT'");}
 @Transactional public Map<String,Object> generateMonthAfterLastPayment(String user){
  membershipChanges.applyDuePlanChanges(LocalDate.now(), user);
  List<Map<String,Object>> memberships=jdbc.queryForList("SELECT id,premium_cents,start_date,end_date,paid_up_to_period FROM membership WHERE status='ACTIVE'");
  int created=0; LocalDate current=LocalDate.now().withDayOfMonth(1);
  for(var m:memberships){
   String paid=Objects.toString(m.get("paid_up_to_period"),"");
   LocalDate target;
   if(paid.matches("\\d{6}")) target=YearMonth.parse(paid,DateTimeFormatter.ofPattern("yyyyMM")).plusMonths(1).atDay(1);
   else target=((java.sql.Date)m.get("start_date")).toLocalDate().withDayOfMonth(1);
   if(target.isAfter(current)) continue;
   Object e=m.get("end_date"); if(e!=null && target.isAfter(((java.sql.Date)e).toLocalDate().withDayOfMonth(1))) continue;
   String period=target.format(DateTimeFormatter.ofPattern("yyyyMM"));
   Long before=jdbc.queryForObject("SELECT COUNT(*) FROM membership_premium WHERE membership_id=? AND period_yyyymm=?",Long.class,m.get("id"),period);
   premiums.findOrCreatePremium(Objects.toString(m.get("id")),period,((Number)m.get("premium_cents")).longValue(),user); if(before!=null&&before==0)created++;
  }
  return Map.of("created",created,"mode","MONTH_AFTER_LAST_PAYMENT");
 }

 @Transactional public Map<String,Object> generate(LocalDate from,LocalDate to,String user){
  membershipChanges.applyDuePlanChanges(LocalDate.now(), user);
  List<Map<String,Object>> memberships=jdbc.queryForList("SELECT id,premium_cents,start_date,end_date,paid_up_to_period FROM membership WHERE status='ACTIVE'"); int created=0;
  for(var m:memberships){ LocalDate p=from.withDayOfMonth(1); LocalDate end=to.withDayOfMonth(1); while(!p.isAfter(end)){
    LocalDate start=((java.sql.Date)m.get("start_date")).toLocalDate().withDayOfMonth(1); Object e=m.get("end_date"); LocalDate stop=e==null?null:((java.sql.Date)e).toLocalDate().withDayOfMonth(1);
    if(!p.isBefore(start)&&(stop==null||!p.isAfter(stop))){String period=p.format(DateTimeFormatter.ofPattern("yyyyMM")); long before=jdbc.queryForObject("SELECT COUNT(*) FROM membership_premium WHERE membership_id=? AND period_yyyymm=?",Long.class,m.get("id"),period); premiums.findOrCreatePremium(Objects.toString(m.get("id")),period,((Number)m.get("premium_cents")).longValue(),user); if(before==0)created++;}
    p=p.plusMonths(1); }} return Map.of("created",created,"from",from.toString(),"to",to.toString());}
}
