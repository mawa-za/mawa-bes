package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CardTerminalRequest;
import za.co.mawa.bes.entity.v2.CardTerminalEntity;
import za.co.mawa.bes.repository.v2.CardTerminalRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.configuration.context.UserContext;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class CardTerminalService {
    private final CardTerminalRepository repository;
    private final UserRepository userRepository;
    public List<CardTerminalEntity> list(boolean activeOnly){ return activeOnly?repository.findByActiveTrueOrderByNameAsc():repository.findAllByOrderByNameAsc(); }
    public CardTerminalEntity requireActive(String id){
        if(id==null||id.isBlank()) throw new IllegalArgumentException("Select the card terminal used for this card payment");
        CardTerminalEntity terminal=repository.findById(id.trim()).orElseThrow(()->new IllegalArgumentException("Card terminal not found"));
        if(!Boolean.TRUE.equals(terminal.getActive())) throw new IllegalStateException("The selected card terminal is inactive");
        return terminal;
    }
    @Transactional public CardTerminalEntity save(String id, CardTerminalRequest request){
        if(request==null||request.getCode()==null||request.getCode().isBlank()) throw new IllegalArgumentException("Terminal code is required");
        if(request.getName()==null||request.getName().isBlank()) throw new IllegalArgumentException("Terminal name is required");
        String code=request.getCode().trim().toUpperCase(Locale.ROOT);
        CardTerminalEntity entity=id==null?new CardTerminalEntity():repository.findById(id).orElseThrow(()->new IllegalArgumentException("Card terminal not found"));
        boolean duplicate = id == null
                ? repository.existsByCodeIgnoreCase(code)
                : repository.existsByCodeIgnoreCaseAndIdNot(code, id);
        if(duplicate) throw new IllegalArgumentException("Card terminal code already exists");
        entity.setCode(code); entity.setName(request.getName().trim());
        entity.setMerchantNumber(trim(request.getMerchantNumber())); entity.setLocationCode(trim(request.getLocationCode()));
        entity.setActive(request.getActive()==null||request.getActive()); entity.setUpdatedBy(trim(request.getChangedBy()));
        if(id==null) entity.setCreatedBy(trim(request.getChangedBy()));
        return repository.save(entity);
    }
    public void validateForPayment(String paymentMethod,String terminalId){ if("CARD".equalsIgnoreCase(paymentMethod)) requireActive(terminalId); }

    public String activeAssignmentOrNull(String terminalId) {
        String normalized = trim(terminalId);
        if (normalized == null) return null;
        return repository.findById(normalized)
                .filter(terminal -> Boolean.TRUE.equals(terminal.getActive()))
                .map(CardTerminalEntity::getId)
                .orElse(null);
    }

    public String resolveForOnlinePayment(String paymentMethod, String suppliedTerminalId) {
        if (!"CARD".equalsIgnoreCase(paymentMethod)) return null;

        UserEntity user = null;
        String userId = UserContext.getCurrentUserId();
        if (StringUtils.hasText(userId)) {
            user = userRepository.findById(userId.trim()).orElse(null);
        }
        if (user == null && StringUtils.hasText(UserContext.getCurrentUser())) {
            user = userRepository.getByName(UserContext.getCurrentUser().trim());
        }

        // ERP online payments must follow the administrator-maintained user assignment.
        // The supplied value is only a compatibility fallback for non-user execution contexts
        // (for example legacy service tests/background integrations).
        String resolvedId = user == null ? trim(suppliedTerminalId) : trim(user.getCardTerminalId());
        if (resolvedId == null) {
            throw new IllegalStateException("No card terminal is assigned to your user. Ask a system administrator to assign one in User Maintenance.");
        }
        return requireActive(resolvedId).getId();
    }

    private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
}
